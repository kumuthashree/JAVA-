package vikaash;

import java.time.LocalDateTime;
import java.util.*;

class Subscriber {
    private static int counter = 1;
    private int id;
    private String name;
    private String email;
    private String status;
    private List<String> tags;

    Subscriber(String name, String email) {
        this.id = counter++;
        this.name = name;
        this.email = email;
        this.status = "ACTIVE";
        this.tags = new ArrayList<>();
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getStatus() { return status; }
    public void deactivate() { this.status = "INACTIVE"; }
    public void activate() { this.status = "ACTIVE"; }
    public List<String> getTags() { return tags; }
    public void addTag(String t) { if (!tags.contains(t)) tags.add(t); }
    public void removeTag(String t) { tags.remove(t); }

    public String toString() {
        return id + ":" + name + "<" + email + "> (" + status + ")";
    }
}

class EmailTemplate {
    private static int counter = 1;
    private int templateId;
    private String name;
    private String subject;
    private String body;
    private List<String> placeholders;

    EmailTemplate(String name, String subject, String body) {
        this.templateId = counter++;
        this.name = name;
        this.subject = subject;
        this.body = body;
        this.placeholders = new ArrayList<>();
    }

    public int getTemplateId() { return templateId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSubject() { return subject; }
    public void setSubject(String s) { this.subject = s; }
    public String getBody() { return body; }
    public void setBody(String b) { this.body = b; }
    public void addPlaceholder(String p) { if (!placeholders.contains(p)) placeholders.add(p); }

    public String render(Map<String, String> vals) {
        String out = body;
        for (String p : placeholders) {
            String key = "{{" + p + "}}";
            out = out.replace(key, vals.getOrDefault(p, ""));
        }
        return out;
    }
}

abstract class Campaign {
    private static int counter = 1;
    protected int campaignId;
    protected EmailTemplate template;
    protected List<Subscriber> audience;
    protected LocalDateTime scheduleTime;
    protected Map<String, Integer> metrics;
    protected boolean sent;
    protected String name;

    Campaign(String name, EmailTemplate template) {
        this.campaignId = counter++;
        this.name = name;
        this.template = template;
        this.audience = new ArrayList<>();
        this.metrics = new HashMap<>();
        this.metrics.put("sent", 0);
        this.metrics.put("opens", 0);
        this.metrics.put("clicks", 0);
        this.metrics.put("unsubs", 0);
        this.sent = false;
    }

    public int getCampaignId() { return campaignId; }
    public String getName() { return name; }
    public EmailTemplate getTemplate() { return template; }
    public List<Subscriber> getAudience() { return audience; }

    public void createAudience(List<Subscriber> subs) {
        this.audience.clear();
        this.audience.addAll(subs);
    }

    public void addToAudience(Subscriber s) {
        if (!audience.contains(s)) audience.add(s);
    }

    public void schedule(LocalDateTime when) { this.scheduleTime = when; }
    public void schedule() { this.scheduleTime = LocalDateTime.now(); }

    public void sendNow() {
        if (sent) return;
        this.sent = true;
        int n = 0;
        for (Subscriber s : audience) {
            if ("ACTIVE".equals(s.getStatus())) n++;
        }
        metrics.put("sent", n);
    }

    protected void updateMetric(String k, int v) {
        metrics.put(k, metrics.getOrDefault(k, 0) + v);
    }

    public Map<String, Integer> getMetrics() { return metrics; }
    public abstract void report();
}

class PromotionalCampaign extends Campaign {
    private String offerCode;
    private String channel;
    private int discountPercent;
    private LocalDateTime createdAt;

    PromotionalCampaign(String name, EmailTemplate template, String offerCode, String channel, int discount) {
        super(name, template);
        this.offerCode = offerCode;
        this.channel = channel;
        this.discountPercent = discount;
        this.createdAt = LocalDateTime.now();
    }

    public void setOfferCode(String c) { this.offerCode = c; }
    public String getOfferCode() { return offerCode; }
    public void setChannel(String ch) { this.channel = ch; }
    public String getChannel() { return channel; }
    public void setDiscount(int d) { this.discountPercent = d; }
    public int getDiscount() { return discountPercent; }

    public void report() {
        int sent = metrics.getOrDefault("sent", 0);
        int opens = metrics.getOrDefault("opens", 0);
        int clicks = metrics.getOrDefault("clicks", 0);
        double openRate = sent == 0 ? 0 : 100.0 * opens / sent;
        double ctr = sent == 0 ? 0 : 100.0 * clicks / sent;
        System.out.println("Promotional Campaign:" + name + " id:" + campaignId + " sent:" + sent +
                " opens:" + opens + " clicks:" + clicks + " openRate%:" + String.format("%.2f", openRate) +
                " CTR%:" + String.format("%.2f", ctr));
    }
}

class TransactionalCampaign extends Campaign {
    private String transactionId;
    private String priority;
    private LocalDateTime createdAt;
    private String source;

    TransactionalCampaign(String name, EmailTemplate template, String transactionId, String priority) {
        super(name, template);
        this.transactionId = transactionId;
        this.priority = priority;
        this.createdAt = LocalDateTime.now();
        this.source = "SYSTEM";
    }

    public void setSource(String s) { this.source = s; }
    public String getSource() { return source; }
    public void setPriority(String p) { this.priority = p; }
    public String getPriority() { return priority; }
    public void setTransactionId(String t) { this.transactionId = t; }
    public String getTransactionId() { return transactionId; }

    public void report() {
        int sent = metrics.getOrDefault("sent", 0);
        int opens = metrics.getOrDefault("opens", 0);
        System.out.println("Transactional Campaign:" + name + " id:" + campaignId + " sent:" + sent + " opens:" + opens);
    }
}

class CampaignService {
    private List<Campaign> campaigns;
    private List<Subscriber> subscribers;
    private List<EmailTemplate> templates;
    private Map<Integer, Campaign> campaignMap;

    CampaignService() {
        campaigns = new ArrayList<>();
        subscribers = new ArrayList<>();
        templates = new ArrayList<>();
        campaignMap = new HashMap<>();
    }

    public void addSubscriber(Subscriber s) { subscribers.add(s); }
    public void addTemplate(EmailTemplate t) { templates.add(t); }

    public Campaign createPromotional(String name, EmailTemplate t, String offer, String channel, int discount) {
        PromotionalCampaign c = new PromotionalCampaign(name, t, offer, channel, discount);
        campaigns.add(c);
        campaignMap.put(c.getCampaignId(), c);
        return c;
    }

    public Campaign createTransactional(String name, EmailTemplate t, String txId, String priority) {
        TransactionalCampaign c = new TransactionalCampaign(name, t, txId, priority);
        campaigns.add(c);
        campaignMap.put(c.getCampaignId(), c);
        return c;
    }

    public List<Subscriber> createAudienceByTag(String tag) {
        List<Subscriber> out = new ArrayList<>();
        for (Subscriber s : subscribers) {
            if (s.getTags().contains(tag) && "ACTIVE".equals(s.getStatus())) out.add(s);
        }
        return out;
    }

    public void schedule(Campaign c, LocalDateTime when) { c.schedule(when); }
    public void schedule(Campaign c) { c.schedule(); }

    public void sendCampaignNow(Campaign c) {
        c.sendNow();
        campaignMap.put(c.getCampaignId(), c);
    }

    public void simulateMetrics(int campaignId, int opens, int clicks, int unsubs) {
        Campaign c = campaignMap.get(campaignId);
        if (c == null) return;
        c.updateMetric("opens", opens);
        c.updateMetric("clicks", clicks);
        c.updateMetric("unsubs", unsubs);
        if (unsubs > 0) {
            for (Subscriber s : c.getAudience()) {
                if (unsubs-- <= 0) break;
                s.deactivate();
            }
        }
    }

    public void renderReports() {
        for (Campaign c : campaigns) {
            c.report();
        }
    }

    public void printSubscriberGrowth() {
        System.out.println("Total Subscribers:" + subscribers.size() +
                " Active:" + subscribers.stream().filter(s -> "ACTIVE".equals(s.getStatus())).count());
    }
}

public class SchedulerAppMain {
    public static void main(String[] args) {
        CampaignService svc = new CampaignService();

        Subscriber s1 = new Subscriber("Alice", "a@x.com");
        s1.addTag("premium");
        Subscriber s2 = new Subscriber("Bob", "b@x.com");
        s2.addTag("trial");
        Subscriber s3 = new Subscriber("Cara", "c@x.com");
        s3.addTag("premium");
        svc.addSubscriber(s1);
        svc.addSubscriber(s2);
        svc.addSubscriber(s3);

        EmailTemplate t1 = new EmailTemplate("PromoSale", "Big Sale", "Hello {{name}}, use {{code}} to get discount");
        t1.addPlaceholder("name");
        t1.addPlaceholder("code");
        svc.addTemplate(t1);

        Campaign promo = svc.createPromotional("HolidaySale", t1, "HOLIDAY21", "Email", 20);
        List<Subscriber> audience = svc.createAudienceByTag("premium");
        promo.createAudience(audience);
        svc.schedule(promo, LocalDateTime.now());
        svc.sendCampaignNow(promo);
        svc.simulateMetrics(promo.getCampaignId(), 50, 10, 1);

        EmailTemplate t2 = new EmailTemplate("OrderConfirm", "Your Order", "Dear {{name}}, your order {{order}} is confirmed");
        t2.addPlaceholder("name");
        t2.addPlaceholder("order");
        svc.addTemplate(t2);

        Campaign tx = svc.createTransactional("OrderConfirmation", t2, "TX123", "HIGH");
        tx.createAudience(Arrays.asList(s1, s2));
        svc.schedule(tx);
        svc.sendCampaignNow(tx);
        svc.simulateMetrics(tx.getCampaignId(), 2, 0, 0);

        svc.renderReports();
        svc.printSubscriberGrowth();
    }
}