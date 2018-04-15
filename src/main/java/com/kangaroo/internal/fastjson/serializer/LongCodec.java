/*
 * Copyright 1999-2017 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kangaroo.internal.fastjson.serializer;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicLong;

import com.kangaroo.internal.fastjson.JSONException;
import com.kangaroo.internal.fastjson.JSONObject;
import com.kangaroo.internal.fastjson.parser.DefaultJSONParser;
import com.kangaroo.internal.fastjson.parser.JSONLexer;
import com.kangaroo.internal.fastjson.parser.JSONToken;
import com.kangaroo.internal.fastjson.parser.deserializer.ObjectDeserializer;

/**
 * @author wenshao[szujobs@hotmail.com]
 */
public class LongCodec implements ObjectSerializer, ObjectDeserializer {

    public static LongCodec instance = new LongCodec();

    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
        SerializeWriter out = serializer.out;

        if (object == null) {
            out.writeNull(SerializerFeature.WriteNullNumberAsZero);
        } else {
            long value = ((Long) object).longValue();
            out.writeLong(value);
    
            if (out.isEnabled(SerializerFeature.WriteClassName) //
                && value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE //
                && fieldType != Long.class
                && fieldType != long.class) {
                out.write('L');
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T> T deserialze(DefaultJSONParser parser, Type clazz, Object fieldName) {
        final JSONLexer lexer = parser.lexer;

        Long longObject;
        try {
            final int token = lexer.token();
            if (token == JSONToken.LITERAL_INT) {
                long longValue = lexer.longValue();
                lexer.nextToken(JSONToken.COMMA);
                longObject = Long.valueOf(longValue);
            } else {
                if (token == JSONToken.LBRACE) {
                    JSONObject jsonObject = new JSONObject(true);
                    parser.parseObject(jsonObject);
                    longObject = com.kangaroo.internal.fastjson.util.TypeUtils.castToLong(jsonObject);
                } else {
                    Object value = parser.parse();

                    longObject = com.kangaroo.internal.fastjson.util.TypeUtils.castToLong(value);
                }
                if (longObject == null) {
                    return null;
                }
            }
        } catch (Exception ex) {
            throw new JSONException("parseLong error, field : " + fieldName, ex);
        }
        
        return clazz == AtomicLong.class //
            ? (T) new AtomicLong(longObject.longValue()) //
            : (T) longObject;
    }

    public int getFastMatchToken() {
        return JSONToken.LITERAL_INT;
    }
}
