package com.turbolearn.backend.utils;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;

public class PdfUtil {

    public static String extractText(String pdfPath){
        try(PDDocument pdDocument = Loader.loadPDF(new File(pdfPath))){
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(pdDocument);
        }
        catch (Exception e){
            e.printStackTrace();
            return "";
        }
    }
}
