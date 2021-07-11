package com.travelFactory.letsVisitTG;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class GetHotelDataForFilters extends HttpServlet{
	
	private static HttpURLConnection connection;
	private static final long serialVersionUID = 1L;
	
	private static Logger logger = Logger.getLogger("GetHotelDataForFilters");
	private static int alreadyConfigured = 0;

    public GetHotelDataForFilters() {
        super();
    }
    
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		//Configuring the logger with the basic configurations
		//TODO: Make a separate configuration file  
		if(alreadyConfigured == 0)
			BasicConfigurator.configure();
		alreadyConfigured = 1;
		
		
		JSONObject apiKey = null;
		//JSON parser object to parse read file
        JSONParser jsonParser = new JSONParser();
        
        //Loading the API key from keys file
        String path = request.getServletContext().getRealPath("/WEB-INF/keys.json");
        try (FileReader reader = new FileReader(path))
        {
            //Read JSON file
            Object obj = jsonParser.parse(reader);
            apiKey = (JSONObject) obj;
 
        } catch (FileNotFoundException e) {
            logger.error("keys.json file does not exist");
        } catch (IOException e) {
            logger.error("IO Exception");
        } catch (ParseException e) {
            logger.error("keys.json file not well formed");
        }
        
        //Check for if apiKey has been read properly or not
        if(apiKey == null) {
            logger.error("Please add the keys.json file in WEB-INF folder");
        	response.setContentType("application/json");
		    response.sendError(500, "Sorry something went wrong from our side, please refresh the page");
		    return;
        }
        
        //Calling the WebHotlier API and sending the appropriate response to the client
		try {
		    URL url = new URL("https://rest.reserve-online.net/availability"+"?"+request.getQueryString());
		    
		    // get data
		    connection = (HttpURLConnection)url.openConnection();
		
		    connection.setRequestMethod("GET");
		    connection.setDoOutput(true);
		    connection.setRequestProperty("Authorization", "Basic " + apiKey.get("apikey"));
		    connection.setRequestProperty("Accept", "application/json");
		    
		    switch (connection.getResponseCode()) {
	            
		    case HttpURLConnection.HTTP_OK:
	            	InputStream content = connection.getInputStream();
	            	BufferedReader in = new BufferedReader(new InputStreamReader(content));
	    		    JSONParser parse = new JSONParser(); 
	    		    apiKey = (JSONObject)parse.parse(in);

	    		    //Check to handle no hotels available case
	    		    if(apiKey.get("error_code").toString().equals("OK")) {
	    		    	response.setContentType("application/json");
		    			response.getWriter().print(apiKey);
		    			logger.info("Hotlier API called, data received, response sent");
	    		    }
	    		    else {
	    		    	response.setContentType("application/json");
		    			response.getWriter().print(apiKey.get("error_code"));
		    			logger.info("Hotlier API called, no hotels available, response sent");
	    		    }
	                break; // fine, go on
	            
	            case HttpURLConnection.HTTP_BAD_REQUEST:
	            	InputStream contentErrorBadRequest = connection.getErrorStream();
	            	BufferedReader inErrorBadRequest = new BufferedReader(new InputStreamReader(contentErrorBadRequest));
	    		    JSONParser parseErrorBadRequest = new JSONParser(); 
	    		    apiKey = (JSONObject)parseErrorBadRequest.parse(inErrorBadRequest);
	    		    response.setContentType("application/json");
	    		    response.sendError(500, apiKey.get("error_msg").toString());
	    		    logger.error("Hotlier API call failed, query parameter not correct:  "+apiKey.get("error_msg").toString());
	    		    break; // badly formatted or missing query parameters
	            
	            case HttpURLConnection.HTTP_FORBIDDEN:
	            	InputStream contentErrorHTTP_FORBIDDEN = connection.getErrorStream();
	            	BufferedReader inErrorHTTP_FORBIDDEN = new BufferedReader(new InputStreamReader(contentErrorHTTP_FORBIDDEN));
	    		    JSONParser parseErrorHTTP_FORBIDDEN = new JSONParser(); 
	    		    apiKey = (JSONObject)parseErrorHTTP_FORBIDDEN.parse(inErrorHTTP_FORBIDDEN);
	    		    response.setContentType("application/json");
	    		    response.sendError(500, apiKey.get("error_msg").toString());
	    		    logger.error("Hotlier API call failed, API key not correct:  "+apiKey.get("error_msg").toString());
	    		    break; // API key authentication failed
	            
	            default:
	            	InputStream contentError = connection.getErrorStream();
	            	BufferedReader inError = new BufferedReader(new InputStreamReader(contentError));
	    		    JSONParser parseError = new JSONParser(); 
	    		    apiKey = (JSONObject)parseError.parse(inError);
	    		    response.setContentType("application/json");
	    		    response.sendError(500, apiKey.get("error_msg").toString());
	    			logger.error("Hotlier API called, some error happend" + apiKey);
	    			// unexpected error happened while calling the API
		    }

		} catch (MalformedURLException e) {
			logger.error("URL is MalFormed" + e.toString());
		} catch (Exception e) {
			logger.error("Some Exception happend" + e.toString());
		}
		finally {
			connection.disconnect();
		}
	}
}
