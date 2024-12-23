package com.cbc.Aggregator.controller;

import com.cbc.Aggregator.service.PropertyLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/emailotp")
public class AggregatorController {


    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PropertyLoader propertyLoader;



    /**
     * POST request forwarder
     * Forwards incoming requests based on the path and payload
     * @param jsonPayload the payload of the request
     * @param path the URL path
     * @param request the HTTP request object
     * @return a ResponseEntity with the forwarded response or an error
     */

    @PostMapping("/{path}")
    public ResponseEntity<Object> forwardRequestCommon(@RequestBody Object jsonPayload, @PathVariable String path, HttpServletRequest request) {
        logger.info("Payload: {}", jsonPayload);

        Map<String, Object> responsePayload = new HashMap<>();
        String targetUrl = propertyLoader.getTargetUrl(path);

        if (targetUrl == null) {
            responsePayload.put("status", "FAILED");
            responsePayload.put("message", "No matching endpoint found for path: " + path);
            logger.info(responsePayload.toString());
            return new ResponseEntity<>(responsePayload, HttpStatus.NOT_FOUND);
        }

        try {

            ResponseEntity<Map> response = restTemplate.postForEntity(targetUrl, jsonPayload, Map.class);

            responsePayload.put("status", response.getBody().get("status"));
            responsePayload.put("message", response.getBody().get("message"));
            logger.info(responsePayload.toString());
            return new ResponseEntity<>(responsePayload, HttpStatus.OK);

        }catch (ResourceAccessException resourceAccessException){
            logger.error("Error while forwarding GET request: ", resourceAccessException);
            responsePayload.put("status", "FAILED");
            responsePayload.put("message", "Oops! The page you requested could not be found.");
            logger.info(responsePayload.toString());
            return new ResponseEntity<>(responsePayload, HttpStatus.NOT_FOUND);

        }
        catch (Exception e) {
            logger.error("Error while forwarding POST request: ", e);
            responsePayload.put("status", "FAILED");
            responsePayload.put("message", "An error occurred while forwarding the request.");
            logger.info(responsePayload.toString());
            return new ResponseEntity<>(responsePayload, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET request forwarder
     * Forwards incoming GET requests based on the path
     * @param headers all headers from the request
     * @param path the URL path
     * @param request the HTTP request object
     * @return a ResponseEntity with the forwarded response or an error
     */
    @GetMapping("/{path}")
    public ResponseEntity<Map<String, Object>> forwardHealthCheck(@RequestHeader Map<String, String> headers, @PathVariable String path, HttpServletRequest request) {

        Map<String, Object> responseMap = new HashMap<>();
        String targetUrl = propertyLoader.getTargetUrl(path);

        if (targetUrl == null) {
            responseMap.put("status", "FAILED");
            responseMap.put("message", "No matching endpoint found for path: " + path);
            logger.info(responseMap.toString());
            return new ResponseEntity<>(responseMap, HttpStatus.NOT_FOUND);
        }

        try {

            HttpHeaders httpHeaders = new HttpHeaders();
            headers.forEach(httpHeaders::set);

            HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
            ResponseEntity<Map> response = restTemplate.exchange(targetUrl, HttpMethod.GET, entity, Map.class);

            responseMap.put("status", "SUCCESS");
            responseMap.put("response", response.getBody() != null ? response.getBody() : "No response body");
            logger.info(responseMap.toString());
            return new ResponseEntity<>(responseMap, HttpStatus.OK);

        }catch (ResourceAccessException resourceAccessException){
            logger.error("Error while forwarding GET request: ", resourceAccessException);
            responseMap.put("status", "FAILED");
            responseMap.put("message", "Oops! The page you requested could not be found.");
            logger.info(responseMap.toString());
            return new ResponseEntity<>(responseMap, HttpStatus.NOT_FOUND);

        }

        catch (Exception e) {
            logger.error("Error while forwarding GET request: ", e);
            responseMap.put("status", "FAILED");
            responseMap.put("message", "An error occurred while forwarding the request.");
            logger.info(responseMap.toString());
            return new ResponseEntity<>(responseMap, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }





}
