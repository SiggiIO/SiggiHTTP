package io.siggi.http.util;

import io.siggi.http.HTTPRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class CompressedStreamProvider {
    public CompressedStreamProvider() {
        addCompressor("gzip", GZIPOutputStream::new);
    }

    private final List<RegisteredCompressor> compressors = new ArrayList<>();

    /**
     * Create a compressed stream out of the supported ones in the Accept-Encoding header, or an uncompressed stream
     * if none of the types are supported in the Accept-Encoding header. The Content-Encoding header will automatically
     * be set in the response.
     *
     * @param request the request you want to create a compressed data stream for.
     * @return an OutputStream you can write data to that will be compressed
     * @throws IOException if something goes wrong
     */
    public OutputStream create(HTTPRequest request) throws IOException {
        Map<String, Double> acceptedTypes = new HashMap<>();
        for (String header : request.getHeaders("Accept-Encoding")) {
            for (String acceptedType : header.split(",")) {
                String[] args = acceptedType.split(";");
                double preference = 1.0;
                for (int i = 1; i < args.length; i++) {
                    if (args[i].startsWith("q=")) {
                        try {
                            preference = Double.parseDouble(args[i].substring(2));
                        } catch (Exception ignored) {
                        }
                    }
                }
                acceptedTypes.put(args[0].trim().toLowerCase(), preference);
            }
        }
        if (acceptedTypes.isEmpty()) return request.response;
        RegisteredCompressor compressor = null;
        double preference = 0.0;
        for (RegisteredCompressor candidate : compressors) {
            if (!acceptedTypes.containsKey(candidate.type)) continue;
            double candidatePreference = acceptedTypes.get(candidate.type);
            if (candidatePreference >= preference) {
                compressor = candidate;
                preference = candidatePreference;
            }
        }
        if (compressor == null) {
            return request.response;
        }
        request.response.setHeader("Content-Encoding", compressor.type);
        return compressor.creator.create(request.response);
    }

    /**
     * Add a compressor to the stream provider. Compressors added later take precedence over one added earlier.
     *
     * @param type The type of the compressor in the Accept-Encoding header.
     * @param creator The constructor for this compressor.
     */
    public void addCompressor(String type, CompressedStreamCreator creator) {
        compressors.add(new RegisteredCompressor(type, creator));
    }

    private static class RegisteredCompressor {
        private final String type;
        private final CompressedStreamCreator creator;

        private RegisteredCompressor(String type, CompressedStreamCreator creator) {
            this.type = type;
            this.creator = creator;
        }
    }

    @FunctionalInterface
    public interface CompressedStreamCreator {
        OutputStream create(OutputStream out) throws IOException;
    }
}
