package com.mikrotasarim.image;

import ij.IJ;
import ij.ImagePlus;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class FrameImage {

    private short[] pixelArray;

    public FrameImage(int[] pixelArray) {
        this.pixelArray = new short[384 * 288];
        for (int i = 0; i < 384 * 288; i++) {
            this.pixelArray[i] = (short) pixelArray[i];
        }
    }

    public void saveTiff(String fileName) throws IOException {
        BufferedImage bufferedImage = new BufferedImage(384, 288, BufferedImage.TYPE_USHORT_GRAY);
        bufferedImage.getRaster().setDataElements(0, 0, 384, 288, pixelArray);
        File file = new File(fileName);
        ImageIO.write(bufferedImage, "TIFF", file);
    }

    public static void saveTiff(int xSize, int ySize, int[] pixels, String fileName) throws IOException {
        short[] shortPixels = new short[xSize * ySize];
        for (int i = 0; i < xSize * ySize; i++) {
            shortPixels[i] = (short) pixels[i];
        }
        BufferedImage bufferedImage = new BufferedImage(384, 288, BufferedImage.TYPE_USHORT_GRAY);
        bufferedImage.getRaster().setDataElements(0, 0, 384, 288, shortPixels);
        File file = new File(fileName);
        ImageIO.write(bufferedImage, "TIFF", file);
    }

    public static void save(BufferedImage image, String fileName) throws IOException {
        File file = new File(fileName);
        ImageIO.write(image, "TIFF", file);
    }

    public static void show(String fileName) {
        ImagePlus img = IJ.openImage(fileName);
        img.show();
        IJ.setMinAndMax(0,16383);
    }

    public static FrameImage fromRaw(byte[] byteArray) {
        int[] pixelArray = new int[384 * 288];
        for (int i = 0; i < 384 * 288; i++) {
            pixelArray[i] = byteArray[2 * i] + byteArray[2 * i + 1] * 256;
        }
        return new FrameImage(pixelArray);
    }

    public static FrameImage fromProcessed(int[] intArray) {
        return new FrameImage(intArray);
    }
}
