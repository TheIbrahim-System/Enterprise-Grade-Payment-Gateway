package com.enterprise.payment.controllers;

import com.enterprise.payment.dtos.CreatePaymentRequest;
import com.enterprise.payment.dtos.PaymentResponse;
import com.enterprise.payment.dtos.RefundRequest;
import com.enterprise.payment.dtos.RefundResponse;
import com.enterprise.payment.services.servicesImp.PaymentServiceImp;
import com.enterprise.payment.services.servicesImp.RefundService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentServiceImp paymentService;
    private final RefundService refundService;

    /**
     * POST /api/v1/payments
     *
     * WHAT IT DOES: Creates a new payment intent.
     * WHAT HAPPENS:
     *  1. @Valid triggers Bean Validation on the request body
     *  2. Extracts authenticated user from SecurityContext (JWT already validated)
     *  3. Extracts client IP for audit logging
     *  4. Delegates to PaymentService.createPayment()
     *  5. Returns 201 Created with payment response
     *
     * @PreAuthorize ensures only ROLE_USER or ROLE_ADMIN can call this.
     */

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String userId = authentication.getName();
        String ipAddress = extractClientIp(httpRequest);

        PaymentResponse response
                = paymentService.createPayment(
                request, userId, ipAddress);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    /**
     * GET /api/v1/payments/{id}
     *
     * WHAT IT DOES: Retrieves a payment by its internal UUID.
     * WHAT HAPPENS: Service fetches from DB. Ownership check ensures
     * users can only view their own payments (ADMIN can see all).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<PaymentResponse>> getPayments(@PathVariable String id,
            Pageable pageable,
            Authentication authentication) {

        Page<PaymentResponse> payments =
                paymentService.getPaymentById(id,
                        authentication.getName(),
                        pageable);

        return ResponseEntity.ok(payments);
    }


//    public ResponseEntity<PaymentResponse> getPayment(
//            @PathVariable String id,
//            Authentication authentication,Pageable pageable) {
//
//        PaymentResponse response = (PaymentResponse) paymentService
//                .getPaymentById(id, pageable)
//        return ResponseEntity.ok(response);
//    }

    /**
     * POST /api/v1/payments/{id}/refund
     *
     * WHAT IT DOES: Initiates a full or partial refund.
     */
    @PostMapping("/{id}/refund")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<RefundResponse> refundPayment(
            @PathVariable String id,
            @Valid @RequestBody RefundRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        request.setPaymentId(id);
        RefundResponse response = refundService.initiateRefund(
                request, authentication.getName(), extractClientIp(httpRequest));
        return ResponseEntity.ok(response);
    }

    /**
     * WHAT IT DOES: Extracts real client IP behind load balancers.
     * WHAT HAPPENS: Checks X-Forwarded-For header first (set by LB/proxy).
     * Falls back to request.getRemoteAddr() if no proxy headers present.
     * IP is stored in audit logs for fraud detection.
     */
    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();  // First IP is the real client
        }
        return request.getRemoteAddr();
    }
}


