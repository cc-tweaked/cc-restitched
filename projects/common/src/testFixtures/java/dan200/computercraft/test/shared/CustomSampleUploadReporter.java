/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.test.shared;

import com.google.auto.service.AutoService;
import dan200.computercraft.shared.computer.upload.FileUpload;
import net.jqwik.api.SampleReportingFormat;

/**
 * Custom jqwik formatters for some of our internal types.
 */
@AutoService(SampleReportingFormat.class)
public class CustomSampleUploadReporter implements SampleReportingFormat {
    @Override
    public boolean appliesTo(Object value) {
        return value instanceof FileUpload;
    }

    @Override
    public Object report(Object value) {
        if (value instanceof FileUpload upload) {
            return String.format("FileUpload(name=%s, contents=%s)", upload.getName(), upload.getBytes());
        } else {
            throw new IllegalStateException("Unexpected value  " + value);
        }
    }
}
