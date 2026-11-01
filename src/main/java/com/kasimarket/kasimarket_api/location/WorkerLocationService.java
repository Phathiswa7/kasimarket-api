package com.kasimarket.kasimarket_api.location;

import com.kasimarket.kasimarket_api.user.User;
import com.kasimarket.kasimarket_api.user.UserRepository;
import com.kasimarket.kasimarket_api.user.VettingStatus;
import com.kasimarket.kasimarket_api.user.WorkerProfile;
import com.kasimarket.kasimarket_api.user.WorkerProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

// The rules for a worker going online (and sharing their position) or offline.
@Service
public class WorkerLocationService {

    private final UserRepository userRepository;
    private final WorkerProfileRepository workerProfileRepository;

    public WorkerLocationService(UserRepository userRepository,
                                 WorkerProfileRepository workerProfileRepository) {
        this.userRepository = userRepository;
        this.workerProfileRepository = workerProfileRepository;
    }

    // The worker goes online, or refreshes their position.
    // The phone calls this again every few minutes while the worker is active.
    @Transactional
    public WorkerLocationView goOnline(String workerEmail, double latitude, double longitude) {
        WorkerProfile profile = requireApprovedWorker(workerEmail);

        profile.setLastLatitude(latitude);
        profile.setLastLongitude(longitude);
        profile.setLocationUpdatedAt(LocalDateTime.now());
        profile.setOnline(true);
        workerProfileRepository.save(profile);

        return new WorkerLocationView(true, profile.getLocationUpdatedAt());
    }

    // The worker goes offline. We ERASE their saved position.
    // Our safety policy: location is kept only while a person is active.
    @Transactional
    public WorkerLocationView goOffline(String workerEmail) {
        WorkerProfile profile = requireApprovedWorker(workerEmail);

        profile.setLastLatitude(null);
        profile.setLastLongitude(null);
        profile.setLocationUpdatedAt(null);
        profile.setOnline(false);
        workerProfileRepository.save(profile);

        return new WorkerLocationView(false, null);
    }

    // Only approved workers can go online.
    private WorkerProfile requireApprovedWorker(String workerEmail) {
        User user = userRepository.findByEmail(workerEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Account not found."));

        WorkerProfile profile = workerProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "No worker profile found."));

        if (profile.getVettingStatus() != VettingStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Your account has not been approved yet.");
        }
        return profile;
    }
}