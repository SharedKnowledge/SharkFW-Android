package net.sharksystem.android.protocols.routing;

import android.content.Context;
import android.util.JsonReader;

import com.vividsolutions.jts.algorithm.ConvexHull;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import net.sharkfw.asip.engine.ASIPInMessage;
import net.sharkfw.asip.engine.ASIPMessage;
import net.sharkfw.asip.engine.ASIPSerializer;
import net.sharkfw.knowledgeBase.SharkKBException;
import net.sharksystem.android.protocols.routing.db.CoordinateContentProvider;
import net.sharksystem.android.protocols.routing.db.CoordinateDTO;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class Utils {

    public static boolean isInMovementProfile(Geometry geometry, Context context) {
        List<CoordinateDTO> allCoordinateDTOs = new CoordinateContentProvider(context).getAllCoordinates();
        Coordinate[] coordinates = new Coordinate[allCoordinateDTOs.size()];
        for (int i = 0; i < allCoordinateDTOs.size(); i++) {
            coordinates[i] = allCoordinateDTOs.get(i).toCoordinate();
        }
        Geometry convexHull = new ConvexHull(coordinates, new GeometryFactory()).getConvexHull();

        return convexHull.intersects(geometry);
    }

    public static String getContent(ASIPInMessage msg) {
        String content = null;
        try {
            if (msg.getCommand() == ASIPMessage.ASIP_INSERT) {
                content = ASIPSerializer.serializeKnowledge(msg.getKnowledge()).toString();
            } else  if (msg.getCommand() == ASIPMessage.ASIP_EXPOSE) {
                content = ASIPSerializer.serializeInterest(msg.getInterest()).toString();
            } else {
                content = getRawContent(msg);
            }
        } catch (SharkKBException | JSONException e) {
            e.printStackTrace();
        }
        return content;
    }

    private static String getRawContent(ASIPInMessage msg) {
        char[] buffer = new char[1024];
        BufferedReader in = new BufferedReader(new InputStreamReader(msg.getRaw(), StandardCharsets.UTF_8));
        StringBuilder builder= new StringBuilder();
        int charsRead;

        try {
            if(in.ready()){
                do{
                    charsRead = in.read(buffer);
                    builder.append(buffer, 0 ,charsRead);
                } while(charsRead == buffer.length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return builder.toString();
    }

    // TODO re-add receivers
    public static String generateMd5Hash(ASIPInMessage msg) {
        try {
            JSONObject jsonObject = new JSONObject(msg.getParsedString());
            jsonObject.remove("TTL");
            jsonObject.remove("RECEIVERS");

            byte[] messageBytes = jsonObject.toString().getBytes(StandardCharsets.UTF_8);

            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(messageBytes);

            byte[] hashBytes = md.digest();

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b & 0xff));
            }

            return sb.toString();
        } catch (JSONException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }
}