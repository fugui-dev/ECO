package com.example.eco.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.http.HttpService;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class Web3jUtil {

    @Resource
    private Web3j web3j;


    public static void main(String[] args) throws IOException {

        String address = "0x8e4B6Fb428f65c8aEF4EfEc73A081249Fbce4493";

        Web3j web3j = Web3j.build(new HttpService("https://bsc-testnet.infura.io/v3/4c223b9e87754809a5d8f819a261fdb7"));

        List input = Arrays.asList(new Uint256(61));

        List output = Arrays.asList(new TypeReference<Uint256>() {
        }, new TypeReference<Uint256>() {
        }, new TypeReference<Uint256>() {
        });

        Function function = new Function("odsNfts", input, output);

        String data = FunctionEncoder.encode(function);

        Transaction transaction = Transaction.createEthCallTransaction(null, address, data);

        EthCall response = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();

        boolean reverted = response.isReverted();

        System.out.println(reverted);

        List<Type> list = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());

        for (Type type : list) {
            System.out.println(type.getValue());
        }
    }
}
