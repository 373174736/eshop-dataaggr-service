package com.lizl.eshop.dataaggr.rabbitmq;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.List;

/**
 *  数据聚合
 * Created by lizhaoliang on 18/2/24.
 */
@Component
@RabbitListener(queues = "aggr-data-change-queue")
public class AggrDataChangeQueueReceiver {
    Logger logger = Logger.getLogger(AggrDataChangeQueueReceiver.class);

    @Autowired
    private JedisPool jedisPool;

    @RabbitHandler
    public void process(String message){
        JSONObject messageJSONObject = JSONObject.parseObject(message);
        logger.info("数据聚合服务收到的消息为:" + message);
        String dimType = messageJSONObject.getString("dim_type");

        if("brand".equals(dimType)){
            processBrandDimDataChange(messageJSONObject);
        }else if("category".equals(dimType)){
            processCategoryDimDataChange(messageJSONObject);
        }else if("product_intro".equals(dimType)){
            processProductIntroDimDataChange(messageJSONObject);
        }else if("product".equals(dimType)){
            processProductDimDataChange(messageJSONObject);
        }

    }

    /**
     * Brand  聚合方法
     * @param messageJSONObject
     */
    private void processBrandDimDataChange(JSONObject messageJSONObject){
        Integer id = messageJSONObject.getInteger("id");
        Jedis jedis = jedisPool.getResource();
        String dataJSON = jedis.get("brand_" + id);

        if(StringUtils.isNotEmpty(dataJSON)){
            jedis.set("dim_brand_" + id, dataJSON);
        }else {
            jedis.del("dim_brand_" + id);
        }
        logger.info("放入redis的数据为 key=" + "dim_brand_" + id +", value=" + jedis.get("dim_brand_" + id));
    }

    /**
     * Category  聚合方法
     * @param messageJSONObject
     */
    private void processCategoryDimDataChange(JSONObject messageJSONObject){
        Integer id = messageJSONObject.getInteger("id");
        Jedis jedis = jedisPool.getResource();
        String dataJSON = jedis.get("category_" + id);

        if(StringUtils.isNotEmpty(dataJSON)){
            jedis.set("dim_category_" + id, dataJSON);
        }else {
            jedis.del("dim_category_" + id);
        }
        logger.info("放入redis的数据为 key=" + "dim_category_" + id +", value=" + jedis.get("dim_category_" + id));
    }

    /**
     * ProductIntro  聚合方法
     * @param messageJSONObject
     */
    private void processProductIntroDimDataChange(JSONObject messageJSONObject){
        Integer id = messageJSONObject.getInteger("id");
        Jedis jedis = jedisPool.getResource();
        String dataJSON = jedis.get("product_intro_" + id);

        if(StringUtils.isNotEmpty(dataJSON)){
            jedis.set("dim_product_intro_" + id, dataJSON);
        }else {
            jedis.del("dim_product_intro_" + id);
        }
        logger.info("放入redis的数据为 key=" + "dim_product_intro_" + id +", value=" + jedis.get("dim_product_intro_" + id));
    }


    /**
     * Product  聚合方法
     * @param messageJSONObject
     */
    private void processProductDimDataChange(JSONObject messageJSONObject){
        Integer id = messageJSONObject.getInteger("id");
        Jedis jedis = jedisPool.getResource();

        List<String> productDataJSONList = jedis.mget("product_" + id, "product_property_" + id, "product_specification_" + id);
        String productDataJSON = productDataJSONList.get(0);
        String productPropertyDataJSON = productDataJSONList.get(1);
        String productSpecificationDataJSON = productDataJSONList.get(2);

        if(StringUtils.isNotEmpty(productDataJSON)){
            JSONObject productDataJSONObject = JSONObject.parseObject(productDataJSON);

            //获取product_property 如果不为空 则拼接到productData中去
            if(StringUtils.isNotEmpty(productPropertyDataJSON)){
                productDataJSONObject.put("product_property", JSONObject.parse(productPropertyDataJSON));

            }
            //获取product_specification 如果不为空 则拼接到productData中去
            if(StringUtils.isNotEmpty(productSpecificationDataJSON)){
                productDataJSONObject.put("product_specification", JSONObject.parse(productSpecificationDataJSON));

            }

            jedis.set("dim_product_" + id,JSONObject.toJSONString(productDataJSONObject));

        }else {
            jedis.del("dim_product_" + id);
        }
        logger.info("放入redis的数据为 key=" + "dim_product_" + id +", value=" + jedis.get("dim_product_" + id));
    }

}
