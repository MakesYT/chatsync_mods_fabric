package top.ncserver.chatimg;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ncserver.chatimg.Tools.Img;
import top.ncserver.chatimg.Tools.dll.ClipboardImage;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class ChatImgClient implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("chatimg");
    public static Map<Integer, Img> imgMap = new LinkedHashMap<Integer, Img>();

    private static boolean isWindows() {
        return System.getProperty("os.name").toUpperCase().contains("WINDOWS");
    }

    private static void copyFile(InputStream inputStream, File file) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            byte[] arrayOfByte = new byte[63];
            int i;
            while ((i = inputStream.read(arrayOfByte)) > 0) {
                fileOutputStream.write(arrayOfByte, 0, i);
            }
            fileOutputStream.close();
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onInitialize() {
        System.out.println("DLL?");
        LOGGER.debug("DLL?");
        if (isWindows()) {
            File dllF = new File("get_clipboard_image.dll");
            if (!dllF.exists()) {
                try {
                    URL url = this.getClass().getClassLoader().getResource("get_clipboard_image.dll");
                    if (url != null) {
                        URLConnection connection = url.openConnection();
                        connection.setUseCaches(false);
                        copyFile(connection.getInputStream(), dllF);
                    }
                } catch (IOException var4) {
                    var4.printStackTrace();
                }

            }
            System.load(dllF.getAbsolutePath());


            LOGGER.debug("DLL加载完成");
            System.out.println("DLL!");
        }
        ClientPlayNetworking.registerGlobalReceiver(Identifier.of("chatimg", "img"), (client, handler, buf, responseSender) -> {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            // 然后我们循环判断是否还有可读数据
            while (buf.isReadable()) {
                // 如果有，我们就从PacketByteBuf中读取一个字节，并写入到字节缓冲区中
                byte b = buf.readByte();
                output.write(b);
            }
            // 最后我们把字节缓冲区转换为字节数组，并转换为字符串
            byte[] bytes = output.toByteArray();
            String message = new String(bytes, StandardCharsets.UTF_8);
            String json = message.substring(message.indexOf("{"));
            //LOGGER.debug(json);
            System.out.println(json);
            try {
                JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
                int imgID = jsonObject.get("id").getAsInt();
                if (ChatImgClient.imgMap.containsKey(imgID)) {
                    Img img = ChatImgClient.imgMap.get(imgID);
                    img.add(jsonObject.get("index").getAsInt(), jsonObject.get("data").getAsString());
                    ChatImgClient.imgMap.replace(imgID, img);
                } else {
                    Img img = new Img(jsonObject.get("packageNum").getAsInt(), jsonObject.get("index").getAsInt(), jsonObject.get("data").getAsString());
                    ChatImgClient.imgMap.put(imgID, img);
                    }
                    Img img = ChatImgClient.imgMap.get(imgID);
                    if (img.allReceived()) {
                        Base64.Decoder decoder = Base64.getDecoder();
                        byte[] b = decoder.decode(img.getData());
                        // 处理数据
                        for (int i = 0; i < b.length; ++i) {
                            if (b[i] < 0) {
                                b[i] += 256;
                            }
                        }
                        NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(b));
                        img.setWidthAndHeight(nativeImage.getWidth(), nativeImage.getHeight());
                        ChatImgClient.imgMap.replace(imgID, img);
                        LOGGER.debug(String.valueOf(imgID));
                        MinecraftClient.getInstance().getTextureManager().registerTexture(Identifier.of("chatimg", "imgs/" + imgID), new NativeImageBackedTexture(nativeImage));

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

        });
    }
}
