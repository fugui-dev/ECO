package com.example.eco.util;

public class TokenTypeUtil {

    public  static String getType(Long tokenId) {

        if (tokenId <= 300) {
            return "OL";
        }

        if (tokenId <= 1000) {
            return "OS";
        }

        if (tokenId <= 6000) {
            return "OA";
        }

        if (tokenId <= 20000) {
            return "OB";
        }

        return "";
    }
}
