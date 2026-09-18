package tv.brisa.app;
import java.util.*;
final class ChatLine {
    final Map<String,String> tags=new HashMap<>();String command="",name="",body="";
    static ChatLine parse(String raw){
        ChatLine m=new ChatLine();String line=raw;
        if(line.startsWith("@")){int end=line.indexOf(' ');if(end<0)return m;for(String tag:line.substring(1,end).split(";")){int eq=tag.indexOf('=');if(eq>=0)m.tags.put(tag.substring(0,eq),unescape(tag.substring(eq+1)));}line=line.substring(end+1);}
        if(line.startsWith(":")){int end=line.indexOf(' ');if(end<0)return m;String prefix=line.substring(1,end);int bang=prefix.indexOf('!');m.name=bang<0?prefix:prefix.substring(0,bang);line=line.substring(end+1);}
        int end=line.indexOf(' ');m.command=end<0?line:line.substring(0,end);int body=line.indexOf(" :");if(body>=0)m.body=line.substring(body+2);
        if(m.body.startsWith("\u0001ACTION ")){m.body=m.body.substring(8);if(m.body.endsWith("\u0001"))m.body=m.body.substring(0,m.body.length()-1);}
        if(!m.tags.getOrDefault("display-name","").isEmpty())m.name=m.tags.get("display-name");return m;
    }
    static String unescape(String value){StringBuilder out=new StringBuilder();for(int i=0;i<value.length();i++){char c=value.charAt(i);if(c=='\\'&&i+1<value.length()){c=value.charAt(++i);switch(c){case 's':c=' ';break;case ':':c=';';break;case 'r':c='\r';break;case 'n':c='\n';break;}}out.append(c);}return out.toString();}
}
