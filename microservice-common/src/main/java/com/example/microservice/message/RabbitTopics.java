package com.example.microservice.message;

public final class RabbitTopics {

    public static final String EXCHANGE = "ideatest.events";

    public static final String PRODUCT_INDEX_QUEUE = "ideatest.product.index";
    public static final String ORDER_EVENT_QUEUE = "ideatest.order.event";
    public static final String STOCK_EVENT_QUEUE = "ideatest.stock.event";

    public static final String PRODUCT_INDEX = "product.index";
    public static final String PRODUCT_DELETE = "product.delete";

    public static final String ORDER_CREATED = "order.created";
    public static final String ORDER_CANCELLED = "order.cancelled";
    public static final String ORDER_STATUS = "order.status";

    public static final String STOCK_DECREASE = "stock.decrease";
    public static final String STOCK_INCREASE = "stock.increase";

    private RabbitTopics() {
    }
}
