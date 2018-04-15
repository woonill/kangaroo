package com.kangaroo.internal.fastjson.serializer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.kangaroo.internal.fastjson.JSON;

public abstract class SerializeFilterable {

    protected List<com.kangaroo.internal.fastjson.serializer.BeforeFilter>       beforeFilters       = null;
    protected List<com.kangaroo.internal.fastjson.serializer.AfterFilter>        afterFilters        = null;
    protected List<com.kangaroo.internal.fastjson.serializer.PropertyFilter>     propertyFilters     = null;
    protected List<com.kangaroo.internal.fastjson.serializer.ValueFilter>        valueFilters        = null;
    protected List<com.kangaroo.internal.fastjson.serializer.NameFilter>         nameFilters         = null;
    protected List<com.kangaroo.internal.fastjson.serializer.PropertyPreFilter>  propertyPreFilters  = null;
    protected List<com.kangaroo.internal.fastjson.serializer.LabelFilter>        labelFilters        = null;
    protected List<com.kangaroo.internal.fastjson.serializer.ContextValueFilter> contextValueFilters = null;

    protected boolean                  writeDirect         = true;

    public List<com.kangaroo.internal.fastjson.serializer.BeforeFilter> getBeforeFilters() {
        if (beforeFilters == null) {
            beforeFilters = new ArrayList<com.kangaroo.internal.fastjson.serializer.BeforeFilter>();
            writeDirect = false;
        }

        return beforeFilters;
    }

    public List<com.kangaroo.internal.fastjson.serializer.AfterFilter> getAfterFilters() {
        if (afterFilters == null) {
            afterFilters = new ArrayList<com.kangaroo.internal.fastjson.serializer.AfterFilter>();
            writeDirect = false;
        }

        return afterFilters;
    }

    public List<com.kangaroo.internal.fastjson.serializer.NameFilter> getNameFilters() {
        if (nameFilters == null) {
            nameFilters = new ArrayList<com.kangaroo.internal.fastjson.serializer.NameFilter>();
            writeDirect = false;
        }

        return nameFilters;
    }

    public List<com.kangaroo.internal.fastjson.serializer.PropertyPreFilter> getPropertyPreFilters() {
        if (propertyPreFilters == null) {
            propertyPreFilters = new ArrayList<com.kangaroo.internal.fastjson.serializer.PropertyPreFilter>();
            writeDirect = false;
        }

        return propertyPreFilters;
    }

    public List<com.kangaroo.internal.fastjson.serializer.LabelFilter> getLabelFilters() {
        if (labelFilters == null) {
            labelFilters = new ArrayList<com.kangaroo.internal.fastjson.serializer.LabelFilter>();
            writeDirect = false;
        }

        return labelFilters;
    }

    public List<com.kangaroo.internal.fastjson.serializer.PropertyFilter> getPropertyFilters() {
        if (propertyFilters == null) {
            propertyFilters = new ArrayList<com.kangaroo.internal.fastjson.serializer.PropertyFilter>();
            writeDirect = false;
        }

        return propertyFilters;
    }

    public List<com.kangaroo.internal.fastjson.serializer.ContextValueFilter> getContextValueFilters() {
        if (contextValueFilters == null) {
            contextValueFilters = new ArrayList<com.kangaroo.internal.fastjson.serializer.ContextValueFilter>();
            writeDirect = false;
        }

        return contextValueFilters;
    }

    public List<com.kangaroo.internal.fastjson.serializer.ValueFilter> getValueFilters() {
        if (valueFilters == null) {
            valueFilters = new ArrayList<com.kangaroo.internal.fastjson.serializer.ValueFilter>();
            writeDirect = false;
        }

        return valueFilters;
    }

    public void addFilter(com.kangaroo.internal.fastjson.serializer.SerializeFilter filter) {
        if (filter == null) {
            return;
        }

        if (filter instanceof com.kangaroo.internal.fastjson.serializer.PropertyPreFilter) {
            this.getPropertyPreFilters().add((com.kangaroo.internal.fastjson.serializer.PropertyPreFilter) filter);
        }

        if (filter instanceof com.kangaroo.internal.fastjson.serializer.NameFilter) {
            this.getNameFilters().add((com.kangaroo.internal.fastjson.serializer.NameFilter) filter);
        }

        if (filter instanceof com.kangaroo.internal.fastjson.serializer.ValueFilter) {
            this.getValueFilters().add((com.kangaroo.internal.fastjson.serializer.ValueFilter) filter);
        }

        if (filter instanceof com.kangaroo.internal.fastjson.serializer.ContextValueFilter) {
            this.getContextValueFilters().add((com.kangaroo.internal.fastjson.serializer.ContextValueFilter) filter);
        }

        if (filter instanceof com.kangaroo.internal.fastjson.serializer.PropertyFilter) {
            this.getPropertyFilters().add((com.kangaroo.internal.fastjson.serializer.PropertyFilter) filter);
        }

        if (filter instanceof com.kangaroo.internal.fastjson.serializer.BeforeFilter) {
            this.getBeforeFilters().add((com.kangaroo.internal.fastjson.serializer.BeforeFilter) filter);
        }

        if (filter instanceof com.kangaroo.internal.fastjson.serializer.AfterFilter) {
            this.getAfterFilters().add((com.kangaroo.internal.fastjson.serializer.AfterFilter) filter);
        }

        if (filter instanceof com.kangaroo.internal.fastjson.serializer.LabelFilter) {
            this.getLabelFilters().add((com.kangaroo.internal.fastjson.serializer.LabelFilter) filter);
        }
    }

    public boolean applyName(com.kangaroo.internal.fastjson.serializer.JSONSerializer jsonBeanDeser, //
                             Object object, String key) {

        if (jsonBeanDeser.propertyPreFilters != null) {
            for (com.kangaroo.internal.fastjson.serializer.PropertyPreFilter filter : jsonBeanDeser.propertyPreFilters) {
                if (!filter.apply(jsonBeanDeser, object, key)) {
                    return false;
                }
            }
        }
        
        if (this.propertyPreFilters != null) {
            for (com.kangaroo.internal.fastjson.serializer.PropertyPreFilter filter : this.propertyPreFilters) {
                if (!filter.apply(jsonBeanDeser, object, key)) {
                    return false;
                }
            }
        }

        return true;
    }
    
    public boolean apply(com.kangaroo.internal.fastjson.serializer.JSONSerializer jsonBeanDeser, //
                         Object object, //
                         String key, Object propertyValue) {
        
        if (jsonBeanDeser.propertyFilters != null) {
            for (com.kangaroo.internal.fastjson.serializer.PropertyFilter propertyFilter : jsonBeanDeser.propertyFilters) {
                if (!propertyFilter.apply(object, key, propertyValue)) {
                    return false;
                }
            }
        }
        
        if (this.propertyFilters != null) {
            for (com.kangaroo.internal.fastjson.serializer.PropertyFilter propertyFilter : this.propertyFilters) {
                if (!propertyFilter.apply(object, key, propertyValue)) {
                    return false;
                }
            }
        }

        return true;
    }
    
    protected String processKey(com.kangaroo.internal.fastjson.serializer.JSONSerializer jsonBeanDeser, //
                                Object object, //
                                String key, //
                                Object propertyValue) {

        if (jsonBeanDeser.nameFilters != null) {
            for (com.kangaroo.internal.fastjson.serializer.NameFilter nameFilter : jsonBeanDeser.nameFilters) {
                key = nameFilter.process(object, key, propertyValue);
            }
        }
        
        if (this.nameFilters != null) {
            for (com.kangaroo.internal.fastjson.serializer.NameFilter nameFilter : this.nameFilters) {
                key = nameFilter.process(object, key, propertyValue);
            }
        }

        return key;
    }
    
    protected Object processValue(com.kangaroo.internal.fastjson.serializer.JSONSerializer jsonBeanDeser, //
                                  com.kangaroo.internal.fastjson.serializer.BeanContext beanContext,
                                  Object object, //
                                  String key, //
                                  Object propertyValue) {

        if (propertyValue != null) {
            if ((jsonBeanDeser.out.writeNonStringValueAsString //
                    || (beanContext != null && (beanContext.getFeatures() & com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteNonStringValueAsString.mask) != 0))
                    && (propertyValue instanceof Number || propertyValue instanceof Boolean)) {
                String format = null;
                if (propertyValue instanceof Number
                        && beanContext != null) {
                    format = beanContext.getFormat();
                }

                if (format != null) {
                    propertyValue = new DecimalFormat(format).format(propertyValue);
                } else {
                    propertyValue = propertyValue.toString();
                }
            } else if (beanContext != null && beanContext.isJsonDirect()) {
                String jsonStr = (String) propertyValue;
                propertyValue = JSON.parse(jsonStr);
            }
        }
        
        if (jsonBeanDeser.valueFilters != null) {
            for (com.kangaroo.internal.fastjson.serializer.ValueFilter valueFilter : jsonBeanDeser.valueFilters) {
                propertyValue = valueFilter.process(object, key, propertyValue);
            }
        }

        List<com.kangaroo.internal.fastjson.serializer.ValueFilter> valueFilters = this.valueFilters;
        if (valueFilters != null) {
            for (com.kangaroo.internal.fastjson.serializer.ValueFilter valueFilter : valueFilters) {
                propertyValue = valueFilter.process(object, key, propertyValue);
            }
        }

        if (jsonBeanDeser.contextValueFilters != null) {
            for (com.kangaroo.internal.fastjson.serializer.ContextValueFilter valueFilter : jsonBeanDeser.contextValueFilters) {
                propertyValue = valueFilter.process(beanContext, object, key, propertyValue);
            }
        }

        if (this.contextValueFilters != null) {
            for (com.kangaroo.internal.fastjson.serializer.ContextValueFilter valueFilter : this.contextValueFilters) {
                propertyValue = valueFilter.process(beanContext, object, key, propertyValue);
            }
        }

        return propertyValue;
    }
    
    /**
     * only invoke by asm byte
     * 
     * @return
     */
    protected boolean writeDirect(com.kangaroo.internal.fastjson.serializer.JSONSerializer jsonBeanDeser) {
        return jsonBeanDeser.out.writeDirect //
               && this.writeDirect //
               && jsonBeanDeser.writeDirect;
    }
}
