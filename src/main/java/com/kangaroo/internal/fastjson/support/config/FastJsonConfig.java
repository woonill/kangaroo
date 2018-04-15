
package com.kangaroo.internal.fastjson.support.config;

import com.kangaroo.internal.fastjson.parser.Feature;
import com.kangaroo.internal.fastjson.parser.ParserConfig;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Config for FastJson.
 *
 * @author VictorZeng
 * @see com.kangaroo.internal.fastjson.serializer.SerializeConfig
 * @see ParserConfig
 * @see com.kangaroo.internal.fastjson.serializer.SerializerFeature
 * @see com.kangaroo.internal.fastjson.serializer.SerializeFilter
 * @see Feature
 * @since 1.2.11
 */

public class FastJsonConfig {

    /**
     * default charset
     */
    private Charset charset;

    /**
     * serializeConfig
     */
    private com.kangaroo.internal.fastjson.serializer.SerializeConfig serializeConfig;

    /**
     * parserConfig
     */
    private ParserConfig parserConfig;

    /**
     * serializerFeatures
     */
    private com.kangaroo.internal.fastjson.serializer.SerializerFeature[] serializerFeatures;

    /**
     * serializeFilters
     */
    private com.kangaroo.internal.fastjson.serializer.SerializeFilter[] serializeFilters;

    /**
     * features
     */
    private Feature[] features;

    /**
     * class level serializeFilter
     */
    private Map<Class<?>, com.kangaroo.internal.fastjson.serializer.SerializeFilter> classSerializeFilters;

    /**
     * format date type
     */
    private String dateFormat;

    protected boolean writeContentLength = true;

    /**
     * init param.
     */
    public FastJsonConfig() {

        this.charset = Charset.forName("UTF-8");

        this.serializeConfig = com.kangaroo.internal.fastjson.serializer.SerializeConfig.getGlobalInstance();
        this.parserConfig = new ParserConfig();

        this.serializerFeatures = new com.kangaroo.internal.fastjson.serializer.SerializerFeature[] {
                com.kangaroo.internal.fastjson.serializer.SerializerFeature.BrowserSecure
        };

        this.serializeFilters = new com.kangaroo.internal.fastjson.serializer.SerializeFilter[0];
        this.features = new Feature[0];
    }

    /**
     * @return the serializeConfig
     */
    public com.kangaroo.internal.fastjson.serializer.SerializeConfig getSerializeConfig() {
        return serializeConfig;
    }

    /**
     * @param serializeConfig the serializeConfig to set
     */
    public void setSerializeConfig(com.kangaroo.internal.fastjson.serializer.SerializeConfig serializeConfig) {
        this.serializeConfig = serializeConfig;
    }

    /**
     * @return the parserConfig
     */
    public ParserConfig getParserConfig() {
        return parserConfig;
    }

    /**
     * @param parserConfig the parserConfig to set
     */
    public void setParserConfig(ParserConfig parserConfig) {
        this.parserConfig = parserConfig;
    }

    /**
     * @return the serializerFeatures
     */
    public com.kangaroo.internal.fastjson.serializer.SerializerFeature[] getSerializerFeatures() {
        return serializerFeatures;
    }

    /**
     * @param serializerFeatures the serializerFeatures to set
     */
    public void setSerializerFeatures(com.kangaroo.internal.fastjson.serializer.SerializerFeature... serializerFeatures) {
        this.serializerFeatures = serializerFeatures;
    }

    /**
     * @return the serializeFilters
     */
    public com.kangaroo.internal.fastjson.serializer.SerializeFilter[] getSerializeFilters() {
        return serializeFilters;
    }

    /**
     * @param serializeFilters the serializeFilters to set
     */
    public void setSerializeFilters(com.kangaroo.internal.fastjson.serializer.SerializeFilter... serializeFilters) {
        this.serializeFilters = serializeFilters;
    }

    /**
     * @return the features
     */
    public Feature[] getFeatures() {
        return features;
    }

    /**
     * @param features the features to set
     */
    public void setFeatures(Feature... features) {
        this.features = features;
    }

    /**
     * @return the classSerializeFilters
     */
    public Map<Class<?>, com.kangaroo.internal.fastjson.serializer.SerializeFilter> getClassSerializeFilters() {
        return classSerializeFilters;
    }

    /**
     * @param classSerializeFilters the classSerializeFilters to set
     */
    public void setClassSerializeFilters(
            Map<Class<?>, com.kangaroo.internal.fastjson.serializer.SerializeFilter> classSerializeFilters) {

        if (classSerializeFilters == null)
            return;

        for (Entry<Class<?>, com.kangaroo.internal.fastjson.serializer.SerializeFilter> entry : classSerializeFilters.entrySet())

            this.serializeConfig.addFilter(entry.getKey(), entry.getValue());

        this.classSerializeFilters = classSerializeFilters;
    }

    /**
     * @return the dateFormat
     */
    public String getDateFormat() {
        return dateFormat;
    }

    /**
     * @param dateFormat the dateFormat to set
     */
    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    /**
     * @return the charset
     */
    public Charset getCharset() {
        return charset;
    }

    /**
     * @param charset the charset to set
     */
    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public boolean isWriteContentLength() {
        return writeContentLength;
    }

    public void setWriteContentLength(boolean writeContentLength) {
        this.writeContentLength = writeContentLength;
    }
}
