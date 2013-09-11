/*
 *
 *  Copyright 2013 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.ice.basic;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.ice.common.AwsUtils;
import com.netflix.ice.common.ConsolidateType;
import com.netflix.ice.common.Poller;
import com.netflix.ice.reader.*;
import com.netflix.ice.reader.ApplicationGroup;
import com.netflix.ice.tag.*;
import org.apache.commons.lang.StringUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer3D;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.TextAnchor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.imageio.ImageIO;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.*;

public class BasicWeeklyCostEmailService extends Poller {

    private ReaderConfig config;

    protected final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy/MM/dd").withZoneUTC();
    protected final DateTimeFormatter linkDateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd hha").withZone(DateTimeZone.UTC);
    protected final NumberFormat numberFormatter = NumberFormat.getNumberInstance(Locale.US);
    protected final NumberFormat percentageFormat = NumberFormat.getPercentInstance();
    protected final NumberFormat costFormatter;

    protected int initDelaySec;
    protected int numWeeks;
    private String urlPrefix;
    private ApplicationGroupService applicationGroupService;
    private String fromEmail;
    private String bccEmail;
    private String testEmail;

    private List<Account> accounts;
    private List<Region> regions;
    private List<Product> products;

    private String headerNote;
    private String throughputMetrics;

    public BasicWeeklyCostEmailService(
            List<Account> accounts,
            List<Region> regions,
            List<Product> products,
            int initDelaySec,
            int numWeeks,
            String urlPrefix,
            ApplicationGroupService applicationGroupService,
            String fromEmail,
            String bccEmail,
            String testEmail) {

        this.accounts = accounts;
        this.regions = regions;
        this.products = products;

        this.initDelaySec = initDelaySec;
        this.numWeeks = numWeeks;
        this.urlPrefix = urlPrefix;
        this.applicationGroupService = applicationGroupService;
        this.fromEmail = fromEmail;
        this.bccEmail = bccEmail;
        this.testEmail = testEmail;

        costFormatter = new NumberFormat() {
            @Override
            public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
                return numberFormatter.format(number, toAppendTo, pos).insert(0, "$");
            }
            @Override
            public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
                return numberFormatter.format(number, toAppendTo, pos).insert(0, "$");
            }
            @Override
            public Number parse(String source, ParsePosition parsePosition) {
                throw new UnsupportedOperationException();
            }
        };

        percentageFormat.setMinimumFractionDigits(1);
        percentageFormat.setMaximumFractionDigits(1);
    }

    @Override
    public void start() {
        config = ReaderConfig.getInstance();
        start(initDelaySec, 7*24*3600, true);
    }

    protected boolean inTest() {
        return false;
    }

    protected String getHeaderNote() {
        return "";
    }

    protected String getThroughputMetrics() throws Exception {
        return "";
    }

    protected String getResourceGroupsDisplayName(String product) {
        if (product.equals(Product.ec2.name))
            return "Applications";
        else if (product.equals(Product.s3.name))
            return "S3 buckets";
        else if (product.equals(Product.rds.name))
            return "RDS DBs";
        else
            return product + " resource groups";
    }

    @Override
    protected void poll() throws Exception {
         trigger(inTest());
    }

    public synchronized void trigger(boolean test) {

        try {
            headerNote = getHeaderNote();
            throughputMetrics = getThroughputMetrics();
            AmazonSimpleEmailServiceClient emailService = AwsUtils.getAmazonSimpleEmailServiceClient();
            Map<String, ApplicationGroup> appgroups = applicationGroupService.getApplicationGroups();
            Map<String, List<ApplicationGroup>> appgroupsByEmail = collectEmails(appgroups);

            for (String email: appgroupsByEmail.keySet()) {
                try {
                    if (!StringUtils.isEmpty(email))
                        sendEmail(test, emailService, email, appgroupsByEmail.get(email));
                }
                catch (Exception e) {
                    logger.error("error in sending email to " + email, e);
                }
            }
        }
        catch (Exception e) {
            logger.error("error sending cost emails", e);
        }
    }

    private static Map<String, List<ApplicationGroup>> collectEmails(Map<String, ApplicationGroup> appgroups) {

        Map<String, List<ApplicationGroup>> result = Maps.newTreeMap();
        for (ApplicationGroup appgroup: appgroups.values()) {
            if (StringUtils.isEmpty(appgroup.owner))
                continue;

            String email = appgroup.owner.trim().toLowerCase();
            List<ApplicationGroup> list = result.get(email);
            if (list == null) {
                list = Lists.newArrayList();
                result.put(email, list);
            }
            list.add(appgroup);
        }

        return result;
    }

    private File createImage(ApplicationGroup appgroup) throws IOException {

        Map<String, Double> costs = Maps.newHashMap();
        DateTime end = new DateTime(DateTimeZone.UTC).withDayOfWeek(1).withMillisOfDay(0);
        Interval interval = new Interval(end.minusWeeks(numWeeks), end);

        for (Product product: products) {
            List<ResourceGroup> resourceGroups = getResourceGroups(appgroup, product);
            if (resourceGroups.size() == 0) {
                continue;
            }
            DataManager dataManager = config.managers.getCostManager(product, ConsolidateType.weekly);
            if (dataManager == null) {
                continue;
            }
            TagLists tagLists = new TagLists(accounts, regions, null, Lists.newArrayList(product), null, null, resourceGroups);
            Map<Tag, double[]> data = dataManager.getData(interval, tagLists, TagType.Product, AggregateType.none, false);
            for (Tag tag: data.keySet()) {
                for (int week = 0; week < numWeeks; week++) {
                    String key = tag + "|" + week;
                    if (costs.containsKey(key))
                        costs.put(key, data.get(tag)[week] + costs.get(key));
                    else
                        costs.put(key, data.get(tag)[week]);
                }
            }
        }

        boolean hasData = false;
        for (Map.Entry<String, Double> entry: costs.entrySet()) {
            if (!entry.getKey().contains("monitor") && entry.getValue() != null && entry.getValue() >= 0.1) {
                hasData = true;
                break;
            }
        }
        if (!hasData)
            return null;

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (Product product: products) {
            for (int week = 0; week < numWeeks; week++) {
                String weekStr = String.format("%s - %s week", formatter.print(end.minusWeeks(numWeeks-week)).substring(5), formatter.print(end.minusWeeks(numWeeks-week-1)).substring(5));
                dataset.addValue(costs.get(product + "|" + week), product.name, weekStr);
            }
        }

        JFreeChart chart = ChartFactory.createBarChart3D(
                appgroup.getDisplayName() + " Weekly AWS Costs",
                "",
                "Costs",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                false,
                false
        );
        CategoryPlot categoryplot = (CategoryPlot) chart.getPlot();
        BarRenderer3D renderer = (BarRenderer3D)categoryplot.getRenderer();
        renderer.setItemLabelAnchorOffset(10.0);
        TextTitle title = chart.getTitle();
        title.setFont(title.getFont().deriveFont((title.getFont().getSize()-3)));

        renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator() {
            public java.lang.String generateLabel(org.jfree.data.category.CategoryDataset dataset, int row, int column) {
                return costFormatter.format(dataset.getValue(row, column));
            }
        });
        renderer.setBaseItemLabelsVisible(true);
        renderer.setBasePositiveItemLabelPosition(new ItemLabelPosition(ItemLabelAnchor.OUTSIDE12, TextAnchor.BASELINE_CENTER));

        NumberAxis numberaxis = (NumberAxis) categoryplot.getRangeAxis();
        numberaxis.setNumberFormatOverride(costFormatter);

        BufferedImage image = chart.createBufferedImage(1200, 400);
        File outputfile = File.createTempFile("awscost", "png");
        ImageIO.write(image, "png", outputfile);

        return outputfile;
    }

    private List<ResourceGroup> getResourceGroups(ApplicationGroup appGroup, Product product) {
        if (product == Product.monitor)
            product = Product.ec2;

        List<List<Product>> products = config.resourceService.getProductsWithResources();
        Product productForResource = null;
        for (List<Product> productList: products) {
            if (productList.contains(product)) {
                productForResource = productList.get(0);
                break;
            }
        }

        if (productForResource == null || appGroup.data.get(productForResource.name) == null)
            return Lists.newArrayList();
        else
            return ResourceGroup.getResourceGroups(appGroup.data.get(productForResource.name));
    }

    private MimeBodyPart constructEmail(int index, ApplicationGroup appGroup, StringBuilder body) throws IOException, MessagingException {

        if (index == 0 && !StringUtils.isEmpty(headerNote))
            body.append(headerNote);

        numberFormatter.setMaximumFractionDigits(1);
        numberFormatter.setMinimumFractionDigits(1);

        File file = createImage(appGroup);

        if (file == null)
            return null;

        DateTime end = new DateTime(DateTimeZone.UTC).withDayOfWeek(1).withMillisOfDay(0);
        String link = getLink("area", ConsolidateType.hourly, appGroup, accounts, regions, end.minusWeeks(numWeeks), end);
        body.append(String.format("<b><h4><a href='%s'>%s</a> Weekly Costs:</h4></b>", link, appGroup.getDisplayName()));

        body.append("<table style=\"border: 1px solid #DDD; border-collapse: collapse\">");
        body.append("<tr style=\"background-color: whiteSmoke;text-align:center\" ><td style=\"border-left: 1px solid #DDD;\"></td>");
        for (int i = 0; i <= accounts.size(); i++) {
            int cols = i == accounts.size() ? 1 : regions.size();
            String accName = i == accounts.size() ? "total" : accounts.get(i).name;
            body.append(String.format("<td style=\"border-left: 1px solid #DDD;font-weight: bold;padding: 4px\" colspan='%d'>", cols)).append(accName).append("</td>");
        }
        body.append("</tr>");
        body.append("<tr style=\"background-color: whiteSmoke;text-align:center\" ><td></td>");
        for (int i = 0; i < accounts.size(); i++) {
            boolean first = true;
            for (Region region: regions) {
                body.append("<td style=\"font-weight: bold;padding: 4px;" + (first ? "border-left: 1px solid #DDD;" : "") + "\">").append(region.name).append("</td>");
                first = false;
            }
        }
        body.append("<td style=\"border-left: 1px solid #DDD;\"></td></tr>");

        Map<String, Double> costs = Maps.newHashMap();

        Interval interval = new Interval(end.minusWeeks(numWeeks), end);
        double[] total = new double[numWeeks];
        for (Product product: products) {
            List<ResourceGroup> resourceGroups = getResourceGroups(appGroup, product);
            if (resourceGroups.size() == 0) {
                continue;
            }
            DataManager dataManager = config.managers.getCostManager(product, ConsolidateType.weekly);
            if (dataManager == null) {
                continue;
            }
            for (int i = 0; i < accounts.size(); i++) {
                List<Account> accountList = Lists.newArrayList(accounts.get(i));
                TagLists tagLists = new TagLists(accountList, regions, null, Lists.newArrayList(product), null, null, resourceGroups);
                Map<Tag, double[]> data = dataManager.getData(interval, tagLists, TagType.Region, AggregateType.none, false);
                for (Tag tag: data.keySet()) {
                    for (int week = 0; week < numWeeks; week++) {
                        String key = accounts.get(i) + "|" + tag + "|" + week;
                        if (costs.containsKey(key))
                            costs.put(key, data.get(tag)[week] + costs.get(key));
                        else
                            costs.put(key, data.get(tag)[week]);
                        total[week] += data.get(tag)[week];
                    }
                }
            }
        }

        boolean firstLine = true;
        DateTime currentWeekEnd = end;
        for (int week = numWeeks-1; week >= 0; week--) {
            String weekStr;
            if (week == numWeeks-1)
                weekStr = "Last week";
            else
                weekStr = (numWeeks-week-1) + " weeks ago";
            String background = week % 2 == 1 ? "background: whiteSmoke;" : "";
            body.append(String.format("<tr style=\"%s\"><td nowrap style=\"border-left: 1px solid #DDD;padding: 4px\">%s (%s - %s)</td>", background, weekStr, formatter.print(currentWeekEnd.minusWeeks(1)).substring(5), formatter.print(currentWeekEnd).substring(5)));
            for (int i = 0; i < accounts.size(); i++) {
                Account account = accounts.get(i);
                for (int j = 0; j < regions.size(); j++) {
                    Region region = regions.get(j);
                    String key = account + "|" + region + "|" + week;
                    double cost = costs.get(key) == null ? 0 : costs.get(key);
                    Double lastCost = week == 0 ? null : costs.get(account + "|" + region + "|" + (week - 1));
                    link = getLink("column", ConsolidateType.daily, appGroup, Lists.newArrayList(account), Lists.newArrayList(region), currentWeekEnd.minusWeeks(1), currentWeekEnd);
                    body.append(getValueCell(cost, lastCost, link, firstLine));
                }
            }
            link = getLink("column", ConsolidateType.daily, appGroup, accounts, regions, currentWeekEnd.minusWeeks(1), currentWeekEnd);
            body.append(getValueCell(total[week], week == 0 ? null : total[week - 1], link, firstLine));
            body.append("</tr>");
            firstLine = false;
            currentWeekEnd = currentWeekEnd.minusWeeks(1);
        }
        body.append("</table>");

        numberFormatter.setMaximumFractionDigits(0);
        numberFormatter.setMinimumFractionDigits(0);

        if (!StringUtils.isEmpty(throughputMetrics))
            body.append(throughputMetrics);

        body.append("<br><img src=\"cid:image_cid_" + index + "\"><br>");
        for (Map.Entry<String, List<String>> entry: appGroup.data.entrySet()) {
            String product = entry.getKey();
            List<String> selected = entry.getValue();
            if (selected == null || selected.size() == 0)
                continue;
            link = getLink("area", ConsolidateType.hourly, appGroup, accounts, regions, end.minusWeeks(numWeeks), end);
            body.append(String.format("<b><h4>%s in <a href='%s'>%s</a>:</h4></b>", getResourceGroupsDisplayName(product), link, appGroup.getDisplayName()));
            for (String name : selected)
                body.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;").append(name).append("<br>");
        }
        body.append("<hr><br>");

        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setFileName(file.getName());
        DataSource ds = new ByteArrayDataSource(new FileInputStream(file), "image/png");
        mimeBodyPart.setDataHandler(new DataHandler(ds));
        mimeBodyPart.setHeader("Content-ID", "<image_cid_" + index + ">");
        mimeBodyPart.setHeader("Content-Disposition", "inline");
        mimeBodyPart.setDisposition(MimeBodyPart.INLINE);

        file.delete();

        return mimeBodyPart;
    }

    private void sendEmail(boolean test, AmazonSimpleEmailServiceClient emailService, String email, List<ApplicationGroup> appGroups)
        throws IOException, MessagingException {

        StringBuilder body = new StringBuilder();
        body.append("<html><head><style type=\"text/css\">a:link, a:visited{color:#006DBA;}a:link, a:visited, a:hover {\n" +
                "text-decoration: none;\n" +
                "}\n" +
                "body {\n" +
                "color: #333;\n" +
                "}" +
                "</style></head>");
        List<MimeBodyPart> mimeBodyParts = Lists.newArrayList();
        int index = 0;
        String subject = "";
        for (ApplicationGroup appGroup: appGroups) {
            boolean hasData = false;
            for (String prodName: appGroup.data.keySet()) {
                if (config.productService.getProductByName(prodName) == null)
                    continue;
                hasData = appGroup.data.get(prodName) != null && appGroup.data.get(prodName).size() > 0;
                if (hasData)
                    break;
            }
            if (!hasData)
                continue;

            try {
                MimeBodyPart mimeBodyPart = constructEmail(index, appGroup, body);
                index++;
                if (mimeBodyPart != null) {
                    mimeBodyParts.add(mimeBodyPart);
                    subject = subject + (subject.length() > 0 ? ", " : "") + appGroup.getDisplayName();
                }
            }
            catch (Exception e) {
                logger.error("Error contructing email", e);
            }
        }
        body.append("</html>");

        if (mimeBodyParts.size() == 0)
            return;

        DateTime end = new DateTime(DateTimeZone.UTC).withDayOfWeek(1).withMillisOfDay(0);
        subject = String.format("%s Weekly AWS Costs (%s - %s)", subject, formatter.print(end.minusWeeks(1)), formatter.print(end));
        String toEmail = test ? testEmail : email;
        Session session = Session.getInstance(new Properties());
        MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setSubject(subject);
        mimeMessage.setRecipients(javax.mail.Message.RecipientType.TO, toEmail);
        if (!test && !StringUtils.isEmpty(bccEmail)) {
            mimeMessage.addRecipients(Message.RecipientType.BCC, bccEmail);
        }
        MimeMultipart mimeMultipart = new MimeMultipart();
        BodyPart p = new MimeBodyPart();
        p.setContent(body.toString(), "text/html");
        mimeMultipart.addBodyPart(p);

        for (MimeBodyPart mimeBodyPart: mimeBodyParts)
            mimeMultipart.addBodyPart(mimeBodyPart);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        mimeMessage.setContent(mimeMultipart);
        mimeMessage.writeTo(outputStream);
        RawMessage rawMessage = new RawMessage(ByteBuffer.wrap(outputStream.toByteArray()));

        SendRawEmailRequest rawEmailRequest = new SendRawEmailRequest(rawMessage);
        rawEmailRequest.setDestinations(Lists.<String>newArrayList(toEmail));
        rawEmailRequest.setSource(fromEmail);
        logger.info("sending email to " + toEmail + " " + body.toString());
        emailService.sendRawEmail(rawEmailRequest);
    }

    private String getValueCell(double value, Double lastValue, String link, boolean doColor) {
        Double diffValue = lastValue != null && lastValue != 0 ? (1.0*value - lastValue) / lastValue : null;
        String color;
        if (diffValue == null || !doColor)
            color = "";
        else if (diffValue <= 0)
            color = "background-color:lightGreen";
        else if (diffValue > 0.2)
            color = "background-color:orangered";
        else
            color = "background-color:orange";
        String diff = diffValue != null ? "(" + (value >= lastValue ? "+" : "-") + percentageFormat.format(Math.abs(diffValue)) + ")" : "";

        return String.format("<td nowrap style=\"border-left: 1px solid #DDD;padding: 4px;%s\"><a href=\"%s\">$%s %s</a></td>", color, link, numberFormatter.format(value), diff);
    }

    private String getLink(String plotType, ConsolidateType consolidateType, ApplicationGroup appgroup, List<Account> accounts, List<Region> regions, DateTime start, DateTime end) {
        String link = urlPrefix + appgroup.getLink() +
                "&plotType=" + plotType +
                "&consolidate=" + consolidateType +
                "&start=" + linkDateFormatter.print(start) +
                "&end=" + linkDateFormatter.print(end) +
                "&groupBy=ResourceGroup";
        if (accounts.size() > 0)
            link += "&account=" + StringUtils.join(accounts, ",");
        if (regions.size() > 0)
            link += "&region=" + StringUtils.join(regions, ",");
        link += "&product=" + StringUtils.join(products, ",");


        return link;
    }
}
