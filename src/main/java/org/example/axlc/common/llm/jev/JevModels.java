package org.example.axlc.common.llm.jev;

import com.google.gson.annotations.SerializedName;
import java.util.Map;
import java.util.List;

/**
 * Jev (System One) Model DTOs
 */
public class JevModels {

    // --- Request DTOs ---

    public static class JevQuestion {
        public String type;
        public String instructions;
        public Object criteria; // Map<String, String> for Choice, List<String> for Score

        public JevQuestion(String type, String instructions) {
            this.type = type;
            this.instructions = instructions;
        }

        public JevQuestion(String type, String instructions, Object criteria) {
            this.type = type;
            this.instructions = instructions;
            this.criteria = criteria;
        }
    }

    public static class JevRequest {
        public String model;
        public Object state; // String, Map, or List
        public Map<String, JevQuestion> questions;

        public JevRequest(String model, Object state, Map<String, JevQuestion> questions) {
            this.model = model;
            this.state = state;
            this.questions = questions;
        }
    }

    // --- Response DTOs ---

    public static class JevResponse {
        public String model;
        public Map<String, JevAnswer> answers;
        public Map<String, Integer> usage;
    }

    public static class JevAnswer {
        public String type;
        
        // Choice fields
        public String choice;
        
        // Noul fields
        public Double noul;
        
        // Score fields
        public Double score;
        
        // Common fields
        public Map<String, Double> probabilities;
        public Double confidence;
    }
}
