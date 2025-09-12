package com.example.eco.common;

import lombok.Getter;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.generated.Uint256;

@Getter
public enum FunctionTypeClassEnum {

    //todo 还有很多 慢慢加
    Address("address",  new TypeReference<DynamicArray<Address>>(){}),

    Uint256("uint256", new TypeReference<DynamicArray<Uint256>>(){});

    private String code;

    private TypeReference type;


    FunctionTypeClassEnum(String code, TypeReference type) {
        this.code = code;
        this.type = type;
    }

    public static FunctionTypeClassEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (FunctionTypeClassEnum status : FunctionTypeClassEnum.values()) {
            if (code.equals(status.code)) {
                return status;
            }
        }
        return null;
    }
}
