package com.example.fooddonation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.fooddonation.entity.DonationDTO;
import com.example.fooddonation.entity.DonationStatus;
import com.example.fooddonation.entity.DonorDTO;
import com.example.fooddonation.entity.NgoDTO;
import com.example.fooddonation.repository.DonationRepository;
import com.example.fooddonation.repository.DonorRepository;
import com.example.fooddonation.repository.NgoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DonationService {

    @Autowired
    private DonationRepository donationRepo;

    @Autowired
    private DonorRepository donorRepo;

    @Autowired
    private NgoRepository ngoRepo;

    @Autowired
    private EmailService emailService;   // ✅ Add Email Service

    public DonationDTO addDonation(int donorId, DonationDTO payload) {

        Optional<DonorDTO> dOpt = donorRepo.findById(donorId);
        if (!dOpt.isPresent()) throw new RuntimeException("Donor not found");

        DonorDTO donor = dOpt.get(); // Needed for email
        payload.setDonor(donor);

        // Attach NGO if exists
        if (payload.getNgo() != null && payload.getNgo().getId() != 0) {
            NgoDTO ngo = ngoRepo.findById(payload.getNgo().getId())
                    .orElseThrow(() -> new RuntimeException("NGO not found"));
            payload.setNgo(ngo);
        } else {
            payload.setNgo(null);
        }

        // ✅ MONEY DONATION AUTOMATIC COMPLETION + EMAIL
        if ("MONEY".equalsIgnoreCase(payload.getDonationType())) {

            // Auto-quantity for money donations
            if (payload.getAmount() != null &&
                (payload.getQuantity() == null || payload.getQuantity().isEmpty())) {
                payload.setQuantity(payload.getAmount());
            }

            // Auto-set donation status timeline
            LocalDateTime now = LocalDateTime.now();
            payload.setStatus(DonationStatus.COMPLETED);
            payload.setConfirmedAt(now);
            payload.setScheduledAt(now);
            payload.setPickedUpAt(now);
            payload.setInTransitAt(now);
            payload.setDeliveredAt(now);
            payload.setCompletedAt(now);
            payload.setStatusMessage(
                "Money donation received successfully. Thank you for your contribution!"
            );

            // SAVE donation first
            DonationDTO savedDonation = donationRepo.save(payload);

            // 👉 NOW SEND EMAIL (Important: send after save)
            String subject = "Donation Receipt - Thank You!";
            String message =
                    "Dear " + donor.getName() + ",\n\n" +
                    "Thank you for your generous donation of ₹" + payload.getAmount() + ".\n" +
                    "Your support helps us serve the community better.\n\n" +
                    "Donation Details:\n" +
                    "Amount: ₹" + payload.getAmount() + "\n" +
                    "NGO: " + (payload.getNgo() != null ? payload.getNgo().getNgoName() : "N/A") + "\n" +
                    "Status: COMPLETED\n\n" +
                    "Regards,\n" +
                    "Food Donation Team";

            // Send email async
            emailService.sendEmail(donor.getEmail(), subject, message);

            return savedDonation;
        }

        // NON-MONEY DONATIONS → Default: Pending
        if (payload.getStatus() == null) {
            payload.setStatus(DonationStatus.PENDING);
            payload.setStatusMessage("Donation added to cart. Waiting for confirmation.");
        }

        return donationRepo.save(payload);
    }

    public DonationDTO updateDonation(int id, int donorId, DonationDTO payload) {
        DonationDTO existing = donationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Donation not found"));

        if (existing.getDonor() == null || existing.getDonor().getId() != donorId) {
            throw new RuntimeException("Not allowed");
        }

        existing.setDonationType(payload.getDonationType());
        existing.setFoodName(payload.getFoodName());
        existing.setMealType(payload.getMealType());
        existing.setCategory(payload.getCategory());
        existing.setQuantity(payload.getQuantity());
        existing.setCity(payload.getCity());
        existing.setExpiryDateTime(payload.getExpiryDateTime());
        existing.setAmount(payload.getAmount());
        existing.setClothesType(payload.getClothesType());
        existing.setItemName(payload.getItemName());

        if (payload.getNgo() != null && payload.getNgo().getId() != 0) {
            NgoDTO ngo = ngoRepo.findById(payload.getNgo().getId())
                    .orElseThrow(() -> new RuntimeException("NGO not found"));
            existing.setNgo(ngo);
        }

        return donationRepo.save(existing);
    }

    public List<DonationDTO> getAllDonations() {
        return donationRepo.findAll();
    }

    public List<DonationDTO> getByDonor(int donorId) {
        return donationRepo.findByDonorId(donorId);
    }

    public List<DonationDTO> getByNgo(int ngoId) {
        return donationRepo.findByNgoId(ngoId);
    }
}
