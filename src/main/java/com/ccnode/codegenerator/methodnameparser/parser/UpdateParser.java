package com.ccnode.codegenerator.methodnameparser.parser;

import com.ccnode.codegenerator.dialog.ParseTypeEnum;
import com.ccnode.codegenerator.methodnameparser.KeyWordConstants;
import com.ccnode.codegenerator.methodnameparser.parsedresult.update.ParsedUpdate;
import com.ccnode.codegenerator.methodnameparser.parsedresult.update.ParsedUpdateDto;
import com.ccnode.codegenerator.methodnameparser.parsedresult.update.ParsedUpdateError;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bruce.ge on 2016/12/12.
 */
public class UpdateParser extends BaseParser {

    private List<ParsedUpdate> parsedUpdates = new ArrayList<>();

    private List<ParsedUpdateError> errors = new ArrayList<>();

    public UpdateParser(String methodName, List<String> props) {
        super(methodName, props);
    }

    public ParsedUpdateDto parse() {
        int state = 0;
        int len = KeyWordConstants.UPDATE.length();
        ParsedUpdate parsedUpdate = new ParsedUpdate();
        parseMethods(state, methodName, len, parsedUpdate);
        ParsedUpdateDto dto = new ParsedUpdateDto();
        dto.setUpdateList(parsedUpdates);
        dto.setErrorList(errors);
        return dto;
    }

    private void parseMethods(int state, String methodName, int len, ParsedUpdate parsedUpdate) {
        if (methodName.length() == len) {
            if (isValidState(state)) {
                parsedUpdates.add(parsedUpdate);
            } else {
                ParsedUpdateError error = new ParsedUpdateError();
                error.setParsedUpdate(parsedUpdate);
                error.setLastState(state);
                error.setRemaining("");
                errors.add(error);
            }
            return;
        }
        String remaining = methodName.substring(len);
        boolean newParseUpdate = false;
        //check with state.

        switch (state) {
            case 0: {
                for (int i = 0; i < props.length; i++) {
                    if (remaining.startsWith(lowerProps[i])) {
                        ParsedUpdate newUpdate = parsedUpdate.clone();
                        newUpdate.addUpdateProps(props[i]);
                        newUpdate.addParsePart(ParseTypeEnum.PROPERTY,props[i]);
                        parseMethods(1, remaining, props[i].length(), newUpdate);
                        newParseUpdate = true;
                    }
                }
                break;
            }
            case 1: {
                if (remaining.startsWith(KeyWordConstants.AND)) {
                    ParsedUpdate newUpdate = parsedUpdate.clone();
                    newUpdate.addParsePart(ParseTypeEnum.AND, KeyWordConstants.AND);
                    parseMethods(0, remaining, KeyWordConstants.AND.length(), newUpdate);
                    newParseUpdate = true;
                }
                if (remaining.startsWith(KeyWordConstants.BY)) {
                    ParsedUpdate newUpdate = parsedUpdate.clone();
                    newUpdate.addParsePart(ParseTypeEnum.BY,KeyWordConstants.BY);
                    parseMethods(2, remaining, KeyWordConstants.BY.length(), newUpdate);
                    newParseUpdate = true;
                }
                break;
            }

            case 2: {
                for (int i = 0; i < props.length; i++) {
                    if (remaining.startsWith(lowerProps[i])) {
                        ParsedUpdate newUpdate = parsedUpdate.clone();
                        newUpdate.addQueryProp(props[i]);
                        newUpdate.addParsePart(ParseTypeEnum.PROPERTY,props[i]);
                        parseMethods(3, remaining, props[i].length(), newUpdate);
                        newParseUpdate = true;
                    }
                }
                break;
            }

            case 3: {
                for (String link : linkOp) {
                    if (remaining.startsWith(link)) {
                        ParsedUpdate newUpdate = parsedUpdate.clone();
                        newUpdate.addConnector(link);
                        newUpdate.addParsePart(ParseTypeEnum.LINKOP,link);
                        parseMethods(2, remaining, link.length(), newUpdate);
                        newParseUpdate = true;
                    }
                }

                for (String comp : compareOp) {
                    if (remaining.startsWith(comp)) {
                        ParsedUpdate newUpdate = parsedUpdate.clone();
                        newUpdate.addQueryOperator(comp);
                        newUpdate.addParsePart(ParseTypeEnum.COMPARATOR, comp);
                        parseMethods(4, remaining, comp.length(), newUpdate);
                        newParseUpdate = true;
                    }
                }
                break;
            }

            case 4: {
                for (String link : linkOp) {
                    if (remaining.startsWith(link)) {
                        ParsedUpdate newUpdate = parsedUpdate.clone();
                        newUpdate.addConnector(link);
                        newUpdate.addParsePart(ParseTypeEnum.LINKOP, link);
                        parseMethods(2, remaining, link.length(), newUpdate);
                        newParseUpdate = true;
                    }
                }
                break;
            }

        }

        if (!newParseUpdate) {
            //means there can find no match for the current info.
            ParsedUpdateError error = new ParsedUpdateError();
            error.setParsedUpdate(parsedUpdate);
            error.setRemaining(remaining);
            error.setLastState(state);
            //no need for the depth. get the remaining is ok.
            errors.add(error);
        }


    }

    private boolean isValidState(int state) {
        if (state == 1 || state == 3 || state == 4) {
            return true;
        }
        return false;
    }
}
