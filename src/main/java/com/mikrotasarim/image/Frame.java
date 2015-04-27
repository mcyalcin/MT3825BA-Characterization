package com.mikrotasarim.image;

import ij.IJ;
import ij.ImagePlus;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Frame {

    private short[] pixelArray;

    public Frame(int[] byteArray) {
        this.pixelArray = new short[384*288];
        for (int i = 0; i < 384*288; i++) {
            pixelArray[i] = (short) byteArray[i];
        }
    }

    public void saveTiff(String fileName) throws IOException {
        BufferedImage bufferedImage = new BufferedImage(384, 288, BufferedImage.TYPE_USHORT_GRAY);
        bufferedImage.getRaster().setDataElements(0,0,384,288, pixelArray);
        File file = new File(filePath + fileName);
        ImageIO.write(bufferedImage, "tiff", file);
    }

    public static void show(String fileName) {
        ImagePlus img = IJ.openImage(filePath + fileName);
        img.show();
    }

    private static String filePath = "/home/mcyalcin/Desktop/";
}
