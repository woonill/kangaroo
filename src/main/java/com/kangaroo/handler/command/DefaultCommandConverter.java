package com.kangaroo.handler.command;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.kangaroo.Request;
import com.kangaroo.util.StrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class DefaultCommandConverter implements CommandConverter {

    public static final String NAME = "commandTranslate";

    private Logger logger = LoggerFactory.getLogger(this.getClass());
//    private FormCommandConverter formCommandConverter = new FormCommandConverter();


    protected final Logger logger() {
        return logger;
    }

    @Override
    public Command convert(Request request) {

        logger.debug("Start Converter request to Command");

        String cType = request.header("Content-Type");
        CommandTranslate ct = null;
        boolean _isSupported = false;

/*        if (FormCommandConverter.isSupport(request)) {
            ct = formCommandConverter.convert(request);
            _isSupported = (ct != null);
        } else if ("application/json".equalsIgnoreCase(cType)
                || "application/json;charset=UTF-8".equalsIgnoreCase(cType)) {

            _isSupported = true;

            String strPayload = new String(request.payload());

            logger.debug("Input Data:" + strPayload);

            JSONObject jo = JSON.parseObject(strPayload);
            if (!StrUtils.isNull(jo.getString(NAME))) {
                String jContents = jo.getString(NAME);
                ct = JSON.parseObject(jContents, CommandTranslate.class);
            } else {
                ct = JSON.parseObject(strPayload, CommandTranslate.class);
            }
        }*/


        _isSupported = true;

        String strPayload = new String(request.payload());

        logger.debug("Input Data:" + strPayload);

        JSONObject jo = JSON.parseObject(strPayload);
        if (!StrUtils.isNull(jo.getString(NAME))) {
            String jContents = jo.getString(NAME);
            ct = JSON.parseObject(jContents, CommandTranslate.class);
        } else {
            ct = JSON.parseObject(strPayload, CommandTranslate.class);
        }

        if (ct == null) {
            if (_isSupported) {
                throw new IllegalArgumentException("Supported type:" + cType + " but not parsing it");
            }
            throw new IllegalArgumentException("not supported type:" + cType);
        }

        final CommandWrapper commandWrapper = toCommand(ct);
        return toCommand2(commandWrapper);
    }

    protected Command toCommand2(CommandWrapper ct) {
        return ct;
    }

    protected CommandWrapper toCommand(CommandTranslate ct) {

        JSONObject jsonObject = new JSONObject();
        if (!StrUtils.isNull(ct.getContents())) {
            logger.debug("Command contents:" + ct.getContents());
            jsonObject = JSON.parseObject(ct.getContents());
        }
        logger.debug("Create DefaultCommand:" + ct.getCommandName() + " properties :" + jsonObject.toJSONString());
        return new DefaultCommandWrapper(ct.getCommandName(), jsonObject);
    }


    public static final class DefaultCommandWrapper implements CommandWrapper {

        private String name;
        private JSONObject data;
        private Logger logger = LoggerFactory.getLogger(this.getClass());

        public DefaultCommandWrapper(String commandName, JSONObject jsonObject) {
            this.name = commandName;
            this.data = jsonObject;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Map<String, Object> toMap() {
            return data;
        }

        @Override
        public <T extends Command> T toCommand(Class<T> cmdClass) {
            this.logger.debug("Start convert data:" + this.data.toJSONString() + " \n to class:" + cmdClass);
            return JSON.parseObject(this.data.toJSONString(), cmdClass);
        }
    }


    public static final class CommandTranslate implements java.io.Serializable {

        private static final long serialVersionUID = 1L;
        private String commandName;
        private String contents;

        public String getCommandName() {
            return commandName;
        }

        public void setCommandName(String commandName) {
            this.commandName = commandName;
        }

        public String getContents() {
            return contents;
        }

        public void setContents(String contents) {
            this.contents = contents;
        }
    }
}
