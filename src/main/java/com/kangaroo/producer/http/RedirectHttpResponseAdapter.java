/*
package com.kangaroo.producer.http;

import com.kangaroo.Header;
import com.kangaroo.Message;
import com.kangaroo.Request;
import com.kangaroo.Response;
import com.kangaroo.util.StrUtils;

*/
/**
 * Created by woonill on 6/2/16.
 *//*

public final class RedirectHttpResponseAdapter implements HttpResponseAdapter.HeaderCreator {


    @Override
    public Header[] call(Request request, Response response) {

        if (Message.REDIRECT_STATUS == response.status()) {
            String location = response.getHeaderVal(Message.REDIRECT_LOCATION_FIELD).orElse(null);
            if (!StrUtils.isNull(location)) {
                return new Header[]{
                        new Header(Message.REDIRECT_LOCATION_FIELD, location)
                };
            }
        }
        return null;
    }
}
*/
