package tv.brisa.app;
import org.junit.Test;import static org.junit.Assert.*;
public class ChatLineTest {
 @Test public void preservesTaggedEmotesAndUnicode(){ChatLine m=ChatLine.parse("@display-name=Foo;emotes=25:2-6,8-12;room-id=42 :foo!foo@tmi PRIVMSG #bar :😀 Kappa Kappa");assertEquals("PRIVMSG",m.command);assertEquals("Foo",m.name);assertEquals("😀 Kappa Kappa",m.body);assertEquals("25:2-6,8-12",m.tags.get("emotes"));}
 @Test public void actionOffsetsStartAfterAction(){assertEquals("Kappa",ChatLine.parse(":a!a@tmi PRIVMSG #b :\u0001ACTION Kappa\u0001").body);}
 @Test public void parsesModerationAndRoom(){assertEquals("42",ChatLine.parse("@room-id=42 :tmi ROOMSTATE #b").tags.get("room-id"));assertEquals("x",ChatLine.parse("@target-msg-id=x :tmi CLEARMSG #b :bad").tags.get("target-msg-id"));}
 @Test public void unescapesNamesWithoutTreatingChatAsMarkup(){assertEquals("a b;c\\d",ChatLine.unescape("a\\sb\\:c\\\\d"));assertEquals("<script>x</script>",ChatLine.parse(":a!a@tmi PRIVMSG #b :<script>x</script>").body);}
}
