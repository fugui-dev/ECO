package com.example.eco.util;

import com.example.odyssey.bean.dto.NftLevelDTO;
import com.example.odyssey.common.NftLevelEnum;
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

    /**
     *
     * @param tokenId
     * @param address 合约地址
     * @return
     */
    public NftLevelDTO getNftIdToLevel(Long tokenId, String address) {

        try {


            List input = Arrays.asList(new Uint256(tokenId));

            List output = Arrays.asList(
                    new TypeReference<Uint256>() {
                    }, new TypeReference<Uint256>() {
                    }, new TypeReference<Uint256>() {
                    }
            );

            Function function = new Function("odsNfts", input, output);

            String data = FunctionEncoder.encode(function);

            Transaction transaction = Transaction.createEthCallTransaction(null, address, data);

            EthCall response = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();

            if (Objects.isNull(response.getValue())) {
                log.error("odsNfts tokenId :{} ,response is error:{}", tokenId, response.getError().getMessage());
                return null;
            }

            List<Type> list = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());

            Long resultTokenId = Long.valueOf(list.get(0).getValue().toString());

            Long resultLevel = Long.valueOf(list.get(1).getValue().toString());

            if (resultLevel == 0){
                log.error("odsNfts tokenId :{} ,result is null", tokenId);
                return null;
            }

            Long resultName = Long.valueOf(list.get(2).getValue().toString());

            NftLevelDTO nftLevelDTO = new NftLevelDTO();
            nftLevelDTO.setTokenId(resultTokenId);
            nftLevelDTO.setLevel(NftLevelEnum.of(resultLevel).getName());
            nftLevelDTO.setName(resultName);

            return nftLevelDTO;



        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

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
