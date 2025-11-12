package com.capelli.capellisaleswindow;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;

public class NumericFilter extends DocumentFilter {

    private boolean isValidDecimal(String text) {
        if (text.isEmpty()) {
            return true;
        }
        // Permite números, opcionalmente un punto o coma, y más números.
        return text.matches("^[0-9]*[.,]?[0-9]*$");
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
        Document doc = fb.getDocument();
        StringBuilder sb = new StringBuilder(doc.getText(0, doc.getLength()));
        sb.insert(offset, string);

        if (isValidDecimal(sb.toString())) {
            super.insertString(fb, offset, string, attr);
        }
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        Document doc = fb.getDocument();
        StringBuilder sb = new StringBuilder(doc.getText(0, doc.getLength()));
        sb.replace(offset, offset + length, text);

        if (isValidDecimal(sb.toString())) {
            super.replace(fb, offset, length, text, attrs);
        }
    }

    @Override
    public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
        Document doc = fb.getDocument();
        StringBuilder sb = new StringBuilder(doc.getText(0, doc.getLength()));
        sb.delete(offset, offset + length);

        // Permitir borrar aunque el resultado intermedio sea inválido (ej. "223." -> "223")
        // O si queda vacío
        if (isValidDecimal(sb.toString()) || sb.length() == 0) {
            super.remove(fb, offset, length);
        }
    }
}