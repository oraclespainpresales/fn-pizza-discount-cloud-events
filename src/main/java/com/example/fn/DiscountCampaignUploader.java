package com.example.fn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.functions.FunctionsInvokeClient;
import com.oracle.bmc.functions.requests.InvokeFunctionRequest;
import com.oracle.bmc.functions.responses.InvokeFunctionResponse;
import com.oracle.bmc.util.StreamUtils;

import io.cloudevents.CloudEvent;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class DiscountCampaignUploader {

    public String handleRequest(CloudEvent event) {
        String responseMess      = "";
        String invokeEndpointURL = "https://gw7unyffbla.eu-frankfurt-1.functions.oci.oraclecloud.com";
        String functionId        = "ocid1.fnfunc.oc1.eu-frankfurt-1.aaaaaaaaack6vdtmj7n2wy3caoljvjvbcuexmvvhm3tp2k7673cg4jj3ir4a";

        try {
            //get upload file properties like namespace or buckername.
            ObjectMapper objectMapper = new ObjectMapper();
            Map data                  = objectMapper.convertValue(event.getData().get(), Map.class);
            Map additionalDetails     = objectMapper.convertValue(data.get("additionalDetails"), Map.class);

            StringBuilder jsonfileUrl = new StringBuilder("https://objectstorage.eu-frankfurt-1.oraclecloud.com/n/")
                    .append(additionalDetails.get("namespace"))
                    .append("/b/")
                    .append(additionalDetails.get("bucketName"))
                    .append("/o/")
                    .append(data.get("resourceName"));
            
            System.out.println("JSON FILE:: " + jsonfileUrl.toString());
            InputStream isJson = new URL(jsonfileUrl.toString()).openStream();

            JSONTokener tokener = new JSONTokener(isJson);
			JSONObject joResult = new JSONObject(tokener);

            JSONArray campaigns = joResult.getJSONArray("campaigns");
            System.out.println("Campaigns:: " + campaigns.length());
			for (int i = 0; i < campaigns.length(); i++) {
                JSONObject obj = campaigns.getJSONObject(i);
                responseMess += invokeCreateCampaingFunction (invokeEndpointURL,functionId,obj.toString());
            }
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
        return responseMess;
    }

    private String invokeCreateCampaingFunction (String invokeEndpointURL, String functionId, String payload) throws IOException {
        String response                            = "";
        AuthenticationDetailsProvider authProvider = new ConfigFileAuthenticationDetailsProvider("/.oci/config","DEFAULT");
        
        //System.out.println("TENANT:: " + authProvider.getTenantId());
        //System.out.println("USER::   " + authProvider.getUserId());
        //System.out.println("FINGER:: " + authProvider.getFingerprint());
        //System.out.println("PATHPK:: " + IOUtils.toString(authProvider.getPrivateKey(), StandardCharsets.UTF_8));

        try (FunctionsInvokeClient fnInvokeClient = new FunctionsInvokeClient(authProvider)){
            fnInvokeClient.setEndpoint(invokeEndpointURL);
            InvokeFunctionRequest ifr = InvokeFunctionRequest.builder()
                    .functionId(functionId)
                    .invokeFunctionBody(StreamUtils.createByteArrayInputStream(payload.getBytes()))
                    .build();

            System.err.println("Invoking function endpoint - " + invokeEndpointURL + " with payload " + payload);
            InvokeFunctionResponse resp = fnInvokeClient.invokeFunction(ifr);
            response = IOUtils.toString(resp.getInputStream(), StandardCharsets.UTF_8);
        }

        return response;
    }
}