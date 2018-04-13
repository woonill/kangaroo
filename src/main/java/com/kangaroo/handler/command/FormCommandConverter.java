/*
package kangaroo.handler.command;*/
/*
package com.kangaroo.handler.command;

import com.alibaba.fastjson.JSON;
import com.kangaroo.Request;
import com.kangaroo.util.StrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.kangaroo.handler.command.DefaultCommandConverter.CommandTranslate;


final class FormCommandConverter {


    public static final String qParmas = "qcmd";
    private static final String formChars = "application/x-www-form-urlencoded;";
    private static final String formCharss = "application/x-www-form-urlencoded";

    private static Logger logger = LoggerFactory.getLogger(FormCommandConverter.class);

    private DefaultCommandConverter dcc;

    public FormCommandConverter() {
    }

    public static boolean isSupport(Request req) {
        String cType = req.header("Content-Type");
        logger.debug("Contents Type[" + cType + "]");
        boolean res = formChars.equalsIgnoreCase(cType)
                || formCharss.equalsIgnoreCase(cType)
                || cType.indexOf(formChars) > 0
                || cType.startsWith(formChars)
                || cType.startsWith("multipart/form-data;");
        return res;
    }

    public CommandTranslate convert(Request request) {

        if (this.isSupport(request)) {

            logger.debug("Start Converter form Request to Command");
            String str = (String) request.toMap().get(qParmas);
            if (StrUtils.isNull(str)) {
                str = (String) request.toMap().get("commandTranslate");
                if (StrUtils.isNull(str)) {
                    logger.debug("Use commandtranslate key to Get Parameter");
                    str = (String) request.toMap().get("commandtranslate");
                }
            }
            logger.debug("The JSON Str:" + str);

            CommandTranslate ct = null;

            if (!StrUtils.isNull(str)) {
                ct = JSON.parseObject(str, CommandTranslate.class);
            } else {
                logger.debug("Use Contents key to Get Parameter");
                str = (String) request.toMap().get("contents");
                String cmdName = (String) request.toMap().get("commandName");
                if (StrUtils.isNull(str) || StrUtils.isNull(cmdName)) {
                    logger.error("not found command name or contents value[" + str + " \n cmd name:" + cmdName + "]");
                    throw new NullPointerException("Command name and Contents is required:");
                }

                ct = new CommandTranslate();
                ct.setCommandName(cmdName);
                ct.setContents(str);
            }
            return ct;
        }
        return null;
    }

}
*/

