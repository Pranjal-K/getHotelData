package com.travelFactory.letsVisitTG;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
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
        // TODO Auto-generated constructor stub
    }
    
    
    
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		if(alreadyConfigured == 0)
			BasicConfigurator.configure();
		alreadyConfigured = 1;
		JSONObject apiKey = null;
		
		//JSON parser object to parse read file
        JSONParser jsonParser = new JSONParser();
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
        
        if(apiKey == null) {
            logger.error("Please add the keys.json file in WEB-INF folder");
        	response.setContentType("application/json");
		    response.sendError(500, "Sorry something went wrong from our side, please refresh the page");
		    return;
        }
        
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
	    		    response.sendError(400, apiKey.get("error_msg").toString());
	    		    logger.error("Hotlier API call failed, query parameter not correct:  "+apiKey.get("error_msg").toString());
	    		    break;
	            case HttpURLConnection.HTTP_FORBIDDEN:
	            	InputStream contentErrorHTTP_FORBIDDEN = connection.getErrorStream();
	            	BufferedReader inErrorHTTP_FORBIDDEN = new BufferedReader(new InputStreamReader(contentErrorHTTP_FORBIDDEN));
	    		    JSONParser parseErrorHTTP_FORBIDDEN = new JSONParser(); 
	    		    apiKey = (JSONObject)parseErrorHTTP_FORBIDDEN.parse(inErrorHTTP_FORBIDDEN);
	    		    response.setContentType("application/json");
	    		    response.sendError(400, apiKey.get("error_msg").toString());
	    		    logger.error("Hotlier API call failed, API key not correct:  "+apiKey.get("error_msg").toString());
	    		    break;
	            default:
	            	System.out.println(" **unknown response code**.");
	            	InputStream contentError = connection.getErrorStream();
	            	BufferedReader inError = new BufferedReader(new InputStreamReader(contentError));
	    		    JSONParser parseError = new JSONParser(); 
	    		    apiKey = (JSONObject)parseError.parse(inError);
	    		    response.setContentType("application/json");
	    			response.getWriter().print(apiKey);
	    			logger.error("Hotlier API called, some error happend" + apiKey);
		    }

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (Exception e) {
		    e.printStackTrace();
		}
		finally {
			connection.disconnect();
		}
	}
}
