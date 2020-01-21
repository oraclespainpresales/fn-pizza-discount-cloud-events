package com.example.fn;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;

import javax.json.Json;
import javax.json.stream.JsonParser;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;

public class DiscountCampaignUploader {

    public String handleRequest(CloudEvent event) {
        try {
            //get upload file properties like namespace or buckername.
            ObjectMapper objectMapper = new ObjectMapper();
            Map data                  = objectMapper.convertValue(event.getData().get(), Map.class);
            Map additionalDetails     = objectMapper.convertValue(data.get("additionalDetails"), Map.class);

            StringBuilder jsonfileUrl = new StringBuilder("https://objectstorage.us-frankfurt-1.oraclecloud.com/n/")
                    .append(additionalDetails.get("namespace"))
                    .append("/b/")
                    .append(additionalDetails.get("bucketName"))
                    .append("/o/")
                    .append(data.get("resourceName"));
            
            System.out.println("JSON FILE:: " + jsonfileUrl.toString());
            InputStream is    = new URL(jsonfileUrl.toString()).openStream();
            JsonParser parser = Json.createParser(is);
            while (parser.hasNext()) {
                JsonParser.Event jsonEvent = parser.next();
                switch(jsonEvent) {
                   case START_ARRAY:
                   case END_ARRAY:
                   case START_OBJECT:
                   case END_OBJECT:
                   case VALUE_FALSE:
                   case VALUE_NULL:
                   case VALUE_TRUE:
                      System.out.println(event.toString());
                      break;
                   case KEY_NAME:
                      System.out.print(event.toString() + " " +
                                       parser.getString() + " - ");
                      break;
                   case VALUE_STRING:
                   case VALUE_NUMBER:
                      System.out.println(event.toString() + " " +
                                         parser.getString());
                      break;
                }
             }
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
        return new String("OK");
    }
}