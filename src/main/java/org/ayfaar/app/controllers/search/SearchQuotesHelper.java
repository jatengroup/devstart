package org.ayfaar.app.controllers.search;

import org.ayfaar.app.model.Item;
import org.ayfaar.app.utils.RegExpUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SearchQuotesHelper {
    public static final int MAX_WORDS_ON_BOUNDARIES = 30;
    private String forCreateLeftPartQuote = "([^\\.\\?!]*)([\\.\\?!]*)(\\.|\\?|\\!)(\\)|\\»)";
    private String forCreateRightPartQuote = "(\\)|\\»)([^\\.\\?!]*)([\\.\\?!]*)";

    public List<Quote> createQuotes(List<Item> foundedItems, List<String> allPossibleSearchQueries) {
        List<Quote> quotes = new ArrayList<Quote>();
        String uri = "ии:пункт:";
        String forLeftPart = "([\\.\\?!]*)([^\\.\\?!]*)(<strong>)";
        String forRightPart = "(<strong>)([^\\.\\?!]*)([\\.\\?!]*)";
        String regexp = createRegExp(allPossibleSearchQueries);

        for (Item item : foundedItems) {
            String content = "";
            Pattern pattern = Pattern.compile("(^" + regexp + ")|(" + RegExpUtils.W + "+" + regexp + RegExpUtils.W +
                    "+)|(" + regexp + "$)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            Matcher matcher = pattern.matcher(item.getContent());

            if (matcher.find()) {
                content = item.getContent().replaceAll("(?iu)\\b(" + regexp + ")\\b", "<strong>$1</strong>");
            }

            String[] phrases = content.split("<strong>");
            String leftPart = getPartQuote(phrases[0] + "<strong>", forLeftPart, "", "left");

            if(leftPart.charAt(0) == '.' || leftPart.charAt(0) == '?' || leftPart.charAt(0) == '!') {
                leftPart = leftPart.substring(1, leftPart.length());
                leftPart = leftPart.trim();
            }
            String[] first = leftPart.split(" ");

            String rightPart = getPartQuote("<strong>" + phrases[phrases.length-1], forRightPart, "", "right");
            String[] last = rightPart.split(" ");

            leftPart = cutSentence(leftPart, first.length - (MAX_WORDS_ON_BOUNDARIES + 1), first.length, "left", first);
            rightPart = cutSentence(rightPart, 0, MAX_WORDS_ON_BOUNDARIES + 1, "right", last);

            String textQuote = createTextQuote(phrases, leftPart, rightPart);

            Quote quote = new Quote();
            quote.setUri(uri + item.getNumber());
            quote.setQuote(textQuote);
            quotes.add(quote);
        }
        return quotes;
    }

    String getPartQuote(String content, String regexp, String text, String flag) {
        Pattern pattern = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher matcher = pattern.matcher(content);

        if(matcher.find()) {
            text = matcher.group();
        }

        if(flag.equals("left")) {
            if (text.charAt(1) == ')' || text.charAt(1) == '»') {
                String temp = text.substring(2, text.length());
                if(content.length() - text.length() > 0) {
                    text = getPartQuote(content.substring(0, (content.length() - text.length()) + 2), forCreateLeftPartQuote, text, "left");
                }
                text += temp;
            }
        }

        if(flag.equals("right") && content.length() > text.length()) {
            if (content.charAt(text.length()) == ')' || content.charAt(text.length()) == '»') {
                text += getPartQuote(content.substring(text.length(), content.length()), forCreateRightPartQuote, text, "right");
            }
        }
        return text;
    }

    private String cutSentence(String text, int startIndex, int endIndex, String flag, String[] words) {
        String partText = "";
        if(words.length > MAX_WORDS_ON_BOUNDARIES + 1) {
            for(int i = startIndex; i < endIndex; i++) {
                partText += words[i] + " ";
            }
            if (flag.equals("left")) {
                partText = partText.trim();
                text = "..." + partText.substring(0, partText.length() - 8).trim();
            }
            if(flag.equals("right")) {
                text = partText.trim() + "...";
            }
        }
        else if(words.length <= MAX_WORDS_ON_BOUNDARIES + 1 && flag.equals("left")) {
            text = text.substring(0, text.length() - 8).trim();
        }
        return text;
    }

    private String createTextQuote(String[] phrases, String firstPart, String lastPart) {
        String textQuote = firstPart;
        for (int i = 1; i < phrases.length - 1; i++) {
            if(!textQuote.isEmpty()) {
                textQuote += textQuote.charAt(textQuote.length()-1) == '-' ? "<strong>" + phrases[i].trim() : " <strong>" + phrases[i].trim();
            }
            else {
                textQuote = "<strong>" + phrases[i].trim();
            }
        }

        if(!textQuote.isEmpty() && textQuote.charAt(textQuote.length()-1) == '-') {
            textQuote += lastPart;
        }
        else {
            textQuote += " " + lastPart;
        }
        return textQuote.trim();
    }

    private String createRegExp(List<String> queries) {
        String reg = "";
        for(String s : queries) {
            reg += s + "|";
        }
        return reg.substring(0, reg.length()-1);
    }
}


