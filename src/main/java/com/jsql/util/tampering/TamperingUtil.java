package com.jsql.util.tampering;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.log4j.Logger;

import com.jsql.model.InjectionModel;

public class TamperingUtil {
    
    /**
     * Log4j logger sent to view.
     */
    private static final Logger LOGGER = Logger.getRootLogger();
    
    public boolean isBase64 = false;
    public boolean isVersionComment = false;
    public boolean isFunctionComment = false;
    public boolean isEqualToLike = false;
    public boolean isRandomCase = false;
    public boolean isHexToChar = false;
    public boolean isQuoteToUtf8 = false;
    public boolean isEval = false;
    public boolean isSpaceToMultilineComment = false;
    public boolean isSpaceToDashComment = false;
    public boolean isSpaceToSharpComment = false;
    
    public String eval = null;

    InjectionModel injectionModel;
    public TamperingUtil(InjectionModel injectionModel) {
        this.injectionModel = injectionModel;
    }

    public void set(
        boolean isBase64,
        boolean isVersionComment,
        boolean isFunctionComment,
        boolean isEqualToLike,
        boolean isRandomCase,
        boolean isHexToChar,
        boolean isQuoteToUtf8,
        boolean isEval,
        boolean isSpaceToMultilineComment,
        boolean isSpaceToDashComment,
        boolean isSpaceToSharpComment
    ) {
        this.isBase64 = isBase64;
        this.isVersionComment = isVersionComment;
        this.isFunctionComment = isFunctionComment;
        this.isEqualToLike = isEqualToLike;
        this.isRandomCase = isRandomCase;
        this.isHexToChar = isHexToChar;
        this.isQuoteToUtf8 = isQuoteToUtf8;
        this.isEval = isEval;
        this.isSpaceToMultilineComment = isSpaceToMultilineComment;
        this.isSpaceToDashComment = isSpaceToDashComment;
        this.isSpaceToSharpComment = isSpaceToSharpComment;
    }
    
    static ScriptEngine nashornEngine = new ScriptEngineManager().getEngineByName("nashorn");
    
    private static String eval(String out, String js) {
        Object result = null;
        
        try {
//            nashornEngine.eval(new FileReader("src/main/resources/random-case.js"));
//            nashornEngine.eval("load('src/main/resources/random-case.js'); var tampering = function(sql) {return "+ js +"}");
            nashornEngine.eval(js);
            
            Invocable invocable = (Invocable) nashornEngine;
            result = invocable.invokeFunction("tampering", out);
        } catch (ScriptException | NoSuchMethodException e) {
            LOGGER.warn(e);
            result = out;
        }
        
        return result.toString();
    }
    
    public String tamper(String in) {
        String sqlQueryDefault = in;
        
        String lead = null;
        String sqlQuery = null;
        String trail = null;
        
        Matcher m = Pattern.compile("(.*SlQqLs)(.*)(lSqQsL.*)").matcher(sqlQueryDefault);
        if (m.find()) {
           lead = m.group(1);
           sqlQuery = m.group(2);
           trail = m.group(3);
        }
        
        if (this.isRandomCase) {
            sqlQuery = eval(sqlQuery, Tampering.RANDOM_CASE.instance().getXmlModel().getJavascript());
        }

        if (this.isHexToChar) {
            sqlQuery = eval(sqlQuery, Tampering.HEX_TO_CHAR.instance().getXmlModel().getJavascript());
        }

        if (this.isFunctionComment) {
            sqlQuery = eval(sqlQuery, Tampering.COMMENT_TO_METHOD_SIGNATURE.instance().getXmlModel().getJavascript());
        }

        if (this.isVersionComment) {
            sqlQuery = eval(sqlQuery, Tampering.VERSIONED_COMMENT_TO_METHOD_SIGNATURE.instance().getXmlModel().getJavascript());
        }
        
        if (this.isEqualToLike) {
            sqlQuery = eval(sqlQuery, Tampering.EQUAL_TO_LIKE.instance().getXmlModel().getJavascript());
        }
        
        // Dependency to: EQUAL_TO_LIKE
        if (this.isSpaceToDashComment) {
            sqlQuery = eval(sqlQuery, Tampering.SPACE_TO_DASH_COMMENT.instance().getXmlModel().getJavascript());
            
        } else if (this.isSpaceToMultilineComment) {
            sqlQuery = eval(sqlQuery, Tampering.SPACE_TO_MULTILINE_COMMENT.instance().getXmlModel().getJavascript());
            
        } else if (this.isSpaceToSharpComment) {
            sqlQuery = eval(sqlQuery, Tampering.SPACE_TO_SHARP_COMMENT.instance().getXmlModel().getJavascript());
        }
        
        if (this.isEval) {
            sqlQuery = eval(sqlQuery, this.eval);
        }
        
        sqlQuery = lead + sqlQuery + trail;
        
        // Include character insertion at the beginning of query
        if (this.isQuoteToUtf8) {
            sqlQuery = eval(sqlQuery, Tampering.QUOTE_TO_UTF8.instance().getXmlModel().getJavascript());
        }
        
        if (this.isBase64) {
            sqlQuery = eval(sqlQuery, Tampering.BASE64.instance().getXmlModel().getJavascript());
        }
        
        // Probl�me si le tag contient des caract�res sp�ciaux
        sqlQuery = sqlQuery.replaceAll("(?i)SlQqLs", "");
        sqlQuery = sqlQuery.replaceAll("(?i)lSqQsL", "");
        
        return sqlQuery;
    }

}
