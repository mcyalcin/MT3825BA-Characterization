package com.mikrotasarim.image;

import ij.IJ;
import ij.ImagePlus;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Frame {

    private short[] pixelArray;

    public Frame(int[] pixelArray) {
        this.pixelArray = new short[384 * 288];
        for (int i = 0; i < 384 * 288; i++) {
            this.pixelArray[i] = (short) pixelArray[i];
//            if(i == 50000) {this.pixelArray[i] = 0;}
//            if(i == 50001) {this.pixelArray[i] = 16383;}
        }
    }

    public void saveTiff(String fileName) throws IOException {
        BufferedImage bufferedImage = new BufferedImage(384, 288, BufferedImage.TYPE_USHORT_GRAY);
        bufferedImage.getRaster().setDataElements(0, 0, 384, 288, pixelArray);
        File file = new File(fileName);
        file.getParentFile().mkdirs();
        file.createNewFile();
        ImageIO.write(bufferedImage, "TIFF", file);
    }

    public static void show(String fileName) {
        ImagePlus img = IJ.openImage(fileName);
        img.show();
        IJ.setMinAndMax(0,16383);
    }

    public static Frame fromRaw(byte[] byteArray) {
        int[] pixelArray = new int[384 * 288];
        for (int i = 0; i < 384 * 288; i++) {
            pixelArray[i] = byteArray[2 * i] + byteArray[2 * i + 1] * 256;
        }
        return new Frame(pixelArray);
    }

    public static Frame fromProcessed(int[] intArray) {
        return new Frame(intArray);
    }
}
