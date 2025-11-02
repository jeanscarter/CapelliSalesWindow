package com.capelli.capellisaleswindow;

import javax.swing.event.DocumentEvent;

public class SimpleDocumentListener implements javax.swing.event.DocumentListener {

    private final Runnable callback;

    public SimpleDocumentListener(Runnable callback) {
        this.callback = callback;
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        callback.run();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        callback.run();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        callback.run();
    }
}