package com.nitish.cricket.cricket_platform.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class ImageUploadService {

    @Autowired
    private Cloudinary cloudinary;

    /**
     * Uploads image to Cloudinary and returns a map containing:
     *  - secure_url (actual image URL)
     *  - public_id (for deletion or future reference)
     */
    public Map<String, String> uploadImage(MultipartFile file, String folder) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "folder", folder,
                        "resource_type", "auto"
                ));

        Map<String, String> result = new HashMap<>();
        result.put("secure_url", uploadResult.get("secure_url").toString());
        result.put("public_id", uploadResult.get("public_id").toString());
        return result;
    }

    /**
     * Deletes image from Cloudinary using its public ID.
     */
    public void deleteImage(String publicId) throws IOException {
        if (publicId != null && !publicId.isEmpty()) {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        }
    }

    public Map<String, String> uploadImageWithId(MultipartFile file, String folder) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), Map.of("folder", folder));

        Map<String, String> result = new HashMap<>();
        result.put("url", uploadResult.get("secure_url").toString());
        result.put("public_id", uploadResult.get("public_id").toString());
        return result;
    }

}
