package com.example.eco.util;

import cn.hutool.json.JSONUtil;
import com.example.eco.bean.dto.TransferFromDTO;
import com.example.eco.common.FunctionTypeClassEnum;
import com.example.eco.model.entity.EtherScanAccountTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
public class InputDataDecoderUtil {

    public static void BscScanAccountTransaction(EtherScanAccountTransaction etherScanAccountTransaction) {

        if (!StringUtils.hasLength(etherScanAccountTransaction.getFunctionName())) {
            return;
        }

        try {
            // 去除方法签名
            String input = etherScanAccountTransaction.getInput().substring(etherScanAccountTransaction.getMethodId().length());

            if (!StringUtils.hasLength(input)){
                return;
            }
            // 解析方法 获取 参数类型
            String functionName = etherScanAccountTransaction.getFunctionName();

            String[] attributeList = functionName.substring(functionName.indexOf("(") + 1, functionName.indexOf(")")).split(", ");

            List<TypeReference<?>> outputParameters = new ArrayList<>();

            List<TransferFromDTO> transferFromList = new ArrayList<>();

            for (String attribute : attributeList) {

                String[] parameter = attribute.split(" ");

                //数组类型
                if (parameter[0].contains("[") && parameter[0].contains("]")) {

                    String type = parameter[0].substring(0, parameter[0].indexOf("["));


                    FunctionTypeClassEnum functionTypeClassEnum = FunctionTypeClassEnum.of(type);
                    if (Objects.isNull(functionTypeClassEnum)) {
                        return;
                    }
                    outputParameters.add(functionTypeClassEnum.getType());

                } else {

                    if (!StringUtils.hasLength(parameter[0])){
                        return;
                    }

                    if (parameter[0].equals("bytes")) {
                        outputParameters.add(TypeReference.create(AbiTypes.getType("bytes32")));
                    } else {
                        outputParameters.add(TypeReference.create(AbiTypes.getType(parameter[0])));
                    }
                }

                TransferFromDTO transferFromDTO = new TransferFromDTO();
                transferFromDTO.setName(parameter[1]);
                transferFromDTO.setType(parameter[0]);

                transferFromList.add(transferFromDTO);

            }

            Function function = new Function(etherScanAccountTransaction.getFunctionName(), new ArrayList<>(), outputParameters);

            List<Type> list = FunctionReturnDecoder.decode(input, function.getOutputParameters());

            for (int i = 0; i < list.size(); i++) {

                TransferFromDTO transferFromDTO = transferFromList.get(i);
                transferFromDTO.setData(JSONUtil.toJsonStr(list.get(i)));
            }

            etherScanAccountTransaction.setDecodedInput(JSONUtil.toJsonStr(transferFromList));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
