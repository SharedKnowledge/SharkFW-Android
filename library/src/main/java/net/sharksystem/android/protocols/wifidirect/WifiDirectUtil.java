package net.sharksystem.android.protocols.wifidirect;

import net.sharkfw.asip.ASIPInterest;
import net.sharkfw.asip.engine.ASIPSerializer;
import net.sharkfw.knowledgeBase.SharkKBException;
import net.sharkfw.knowledgeBase.inmemory.InMemoSharkKB;

import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by j4rvis on 28.07.16.
 */
public class WifiDirectUtil {

    public final static String NAME_RECORD = "NAME";
    public final static String TOPIC_RECORD = "TO";
    public final static String TYPE_RECORD = "TY";
    public final static String SENDER_RECORD = "SE";
    public final static String APPROVERS_RECORD = "AP";
    public final static String RECEIVER_RECORD = "RE";
    public final static String LOCATION_RECORD = "LO";
    public final static String TIME_RECORD = "TI";
    public final static String DIRECTION_RECORD = "DI";

    public static HashMap<String, String> interest2RecordMap(ASIPInterest space){

        HashMap<String, String> map = new HashMap<>();

        String serializedTopic = "";
        String serializedType = "";
        String serializedSender = "";
        String serializedApprovers = "";
        String serializedReceiver = "";
        String serializedLocation = "";
        String serializedTime = "";
        int direction = -1;
        String name = "";

        try {

            if(space.getTopics() != null){
                serializedTopic = ASIPSerializer.serializeSTSet(space.getTopics()).toString();
            }
            if(space.getTypes() == null){
                serializedType = ASIPSerializer.serializeSTSet(space.getTypes()).toString();
            }
            if(space.getSender() == null){
                serializedSender = ASIPSerializer.serializeTag(space.getSender()).toString();
                name = space.getSender().getName();
                if(name.isEmpty()) {
                    name = "A";
                }
            }
            if(space.getApprovers() == null){
                serializedApprovers = ASIPSerializer.serializeSTSet(space.getApprovers()).toString();
            }
            if(space.getReceivers() == null){
                serializedReceiver = ASIPSerializer.serializeSTSet(space.getReceivers()).toString();
            }
            if(space.getLocations() == null){
                serializedLocation = ASIPSerializer.serializeSTSet(space.getLocations()).toString();
            }
            if(space.getTimes() == null){
                serializedTime = ASIPSerializer.serializeSTSet(space.getTimes()).toString();
            }
            if(space.getDirection() < 0){
                direction = space.getDirection();
            }

        } catch (SharkKBException | JSONException e) {
            e.printStackTrace();
        }

        map.put(NAME_RECORD, name);
        map.put(TOPIC_RECORD, serializedTopic);
        map.put(TYPE_RECORD, serializedType);
        map.put(SENDER_RECORD, serializedSender);
        map.put(APPROVERS_RECORD, serializedApprovers);
        map.put(RECEIVER_RECORD, serializedReceiver);
        map.put(LOCATION_RECORD, serializedLocation);
        map.put(TIME_RECORD, serializedTime);
        map.put(DIRECTION_RECORD, String.valueOf(direction));

        return map;
    }

    public static ASIPInterest recordMap2Interest(Map<String, String> map) throws SharkKBException {

        ASIPInterest interest = InMemoSharkKB.createInMemoASIPInterest();

        interest.setTopics(InMemoSharkKB.createInMemoSTSet());
        interest.setTypes(InMemoSharkKB.createInMemoSTSet());
        interest.setApprovers(InMemoSharkKB.createInMemoPeerSTSet());
        interest.setReceivers(InMemoSharkKB.createInMemoPeerSTSet());
        interest.setLocations(InMemoSharkKB.createInMemoSpatialSTSet());
        interest.setTimes(InMemoSharkKB.createInMemoTimeSTSet());

        if(map.containsKey(TOPIC_RECORD)){
            String record = map.get(TOPIC_RECORD);
            interest.getTopics().merge(ASIPSerializer.deserializeSTSet(record));
        }
        if(map.containsKey(TYPE_RECORD)){
            String record = map.get(TYPE_RECORD);
            interest.getTypes().merge(ASIPSerializer.deserializeSTSet(record));
        }
        if(map.containsKey(SENDER_RECORD)){
            String record = map.get(SENDER_RECORD);
            interest.setSender(ASIPSerializer.deserializePeerTag(record));
        }
        if(map.containsKey(APPROVERS_RECORD)){
            String record = map.get(APPROVERS_RECORD);
            interest.getApprovers().merge(ASIPSerializer.deserializePeerSTSet(null, record));
        }
        if(map.containsKey(RECEIVER_RECORD)){
            String record = map.get(RECEIVER_RECORD);
            interest.getReceivers().merge(ASIPSerializer.deserializePeerSTSet(null, record));
        }
        if(map.containsKey(LOCATION_RECORD)){
            String record = map.get(LOCATION_RECORD);
            interest.getLocations().merge(ASIPSerializer.deserializeSpatialSTSet(null, record));
        }
        if(map.containsKey(TIME_RECORD)){
            String record = map.get(TIME_RECORD);
            interest.getTimes().merge(ASIPSerializer.deserializeTimeSTSet(null, record));
        }
        if(map.containsKey(DIRECTION_RECORD)){
            int record = Integer.getInteger(map.get(DIRECTION_RECORD));
            interest.setDirection(record);
        }
        return interest;
    }

    public static boolean isValidRecordMap(Map<String, String> map){
        if(map.containsKey(NAME_RECORD)
                && map.containsKey(TOPIC_RECORD)){
            return true;
        } else {
            return false;
        }
    }
}
