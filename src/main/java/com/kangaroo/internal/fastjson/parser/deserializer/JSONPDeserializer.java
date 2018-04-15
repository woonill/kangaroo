package com.kangaroo.internal.fastjson.parser.deserializer;

import com.kangaroo.internal.fastjson.JSONException;
import com.kangaroo.internal.fastjson.JSONPObject;
import com.kangaroo.internal.fastjson.parser.JSONToken;

import java.lang.reflect.Type;

/**
 * Created by wenshao on 21/02/2017.
 */
public class JSONPDeserializer implements ObjectDeserializer {
    public static final JSONPDeserializer instance = new JSONPDeserializer();

    public <T> T deserialze(com.kangaroo.internal.fastjson.parser.DefaultJSONParser parser, Type type, Object fieldName) {
        com.kangaroo.internal.fastjson.parser.JSONLexerBase lexer = (com.kangaroo.internal.fastjson.parser.JSONLexerBase) parser.getLexer();

        com.kangaroo.internal.fastjson.parser.SymbolTable symbolTable = parser.getSymbolTable();

        String funcName = lexer.scanSymbolUnQuoted(symbolTable);
        lexer.nextToken();

        int tok = lexer.token();

        if (tok == com.kangaroo.internal.fastjson.parser.JSONToken.DOT) {
            String name = lexer.scanSymbolUnQuoted(parser.getSymbolTable());
            funcName += ".";
            funcName += name;
            lexer.nextToken();
            tok = lexer.token();
        }

        JSONPObject jsonp = new JSONPObject(funcName);

        if (tok != com.kangaroo.internal.fastjson.parser.JSONToken.LPAREN) {
            throw new JSONException("illegal jsonp : " + lexer.info());
        }
        lexer.nextToken();
        for (;;) {
            Object arg = parser.parse();
            jsonp.addParameter(arg);

            tok = lexer.token();
            if (tok == com.kangaroo.internal.fastjson.parser.JSONToken.COMMA) {
                lexer.nextToken();
            } else if (tok == com.kangaroo.internal.fastjson.parser.JSONToken.RPAREN) {
                lexer.nextToken();
                break;
            } else {
                throw new JSONException("illegal jsonp : " + lexer.info());
            }
         }
        tok = lexer.token();
        if (tok == JSONToken.SEMI) {
            lexer.nextToken();
        }

        return (T) jsonp;
    }

    public int getFastMatchToken() {
        return 0;
    }
}
