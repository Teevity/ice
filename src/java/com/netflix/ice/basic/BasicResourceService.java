package com.netflix.ice.basic;

import com.google.common.collect.Lists;
import com.netflix.ice.common.ResourceService;
import com.netflix.ice.processor.ProcessorConfig;
import com.netflix.ice.reader.ReaderConfig;
import com.netflix.ice.tag.Account;
import com.netflix.ice.tag.Product;
import com.netflix.ice.tag.Region;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public class BasicResourceService extends ResourceService {

    private ProcessorConfig processorConfig;

    @Override
    public void init() {
        processorConfig = ProcessorConfig.getInstance();
    }

    @Override
    public String getResource(Account account, Region region, Product product, String resourceId, String[] lineItem, long millisStart) {
        List<String> header = processorConfig.lineItemProcessor.getHeader();

        String result = "";
        for (String tag: processorConfig.customTags) {
            int index = header.indexOf(tag);
            if (index > 0 && lineItem.length > index && !StringUtils.isEmpty(lineItem[index]))
                result = StringUtils.isEmpty(result) ? lineItem[index] : result + "_" + lineItem[index];
        }

        return StringUtils.isEmpty(result) ? product.name : result;
    }

    @Override
    public List<List<Product>> getProductsWithResources() {
        List<List<Product>> result = Lists.newArrayList();
        for (Product product: ReaderConfig.getInstance().productService.getProducts()) {
            result.add(Lists.<Product>newArrayList(product));
        }
        return result;
    }

    @Override
    public void commit() {

    }
}
