package com.reactiverates.application;

import com.reactiverates.domain.model.HistoricalExchangeRate;
import com.reactiverates.domain.service.HistoricalRateProvider;
import com.reactiverates.domain.service.HistoricalRateRepository;
import com.reactiverates.domain.service.HistoricalRateService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DefaultHistoricalRateService implements HistoricalRateService {

    private static final Logger log = LoggerFactory.getLogger(DefaultHistoricalRateService.class);

    private final HistoricalRateRepository repository;
    private final HistoricalRateProvider provider;

    public DefaultHistoricalRateService(
            HistoricalRateRepository repository,
            HistoricalRateProvider provider) {
        this.repository = repository;
        this.provider = provider;
        log.info("DefaultHistoricalRatesService initialized with repository: {} and {} historical provider",
                repository.getClass().getSimpleName(), provider.getProviderName());
    }

    @Override
    public Flux<HistoricalExchangeRate> getHistoricalRates(
            String fromCurrency, 
            String toCurrency, 
            LocalDate startDate, 
            LocalDate endDate) {
        
        log.debug("Getting historical rates: {} -> {} from {} to {}", 
            fromCurrency, toCurrency, startDate, endDate);
        
        if (startDate.isAfter(endDate)) {
            return Flux.error(new IllegalArgumentException("Start date cannot be after end date"));
        }
        
        if (startDate.isAfter(LocalDate.now())) {
            return Flux.error(new IllegalArgumentException("Cannot request future dates"));
        }
        
        return repository.findByPeriod(fromCurrency, toCurrency, startDate, endDate)
            .collectList()
            .flatMapMany(dbRates -> {
                Set<LocalDate> existingDates = dbRates.stream()
                    .map(HistoricalExchangeRate::date).collect(Collectors.toSet());

                Set<LocalDate> missingDates = findMissingBusinessDates(startDate, endDate, existingDates);

                Flux<HistoricalExchangeRate> fetchedRates;
                if (missingDates.isEmpty()) {
                    log.info("All rates for {}->{} are already in cache.", fromCurrency, toCurrency);
                    fetchedRates = Flux.empty();
                }
                else {
                    log.info("Fetching {} missing rates for {}->{}", missingDates.size(), fromCurrency, toCurrency);
                    fetchedRates = fetchAndSaveRates(fromCurrency, toCurrency, missingDates);
                }

                return Flux.fromIterable(dbRates)
                    .concatWith(fetchedRates).sort(Comparator.comparing(HistoricalExchangeRate::date));
            });
    }

    private Flux<HistoricalExchangeRate> fetchAndSaveRates(String from, String to, Set<LocalDate> dates) {
        Flux<HistoricalExchangeRate> fetchedStream = provider.getHistoricalRatesForDates(from, to, dates)
            .doOnNext(rate -> log.debug("Fetched from API: {}", rate));

        return repository.saveAll(fetchedStream)
            .doOnNext(saved -> log.debug("Saved to DB: {}", saved))
            .onErrorContinue((err, obj) -> log.warn("Failed to save rate: {}. Details: {}", obj, err.getMessage()));
    }

    private Set<LocalDate> findMissingBusinessDates(LocalDate start, LocalDate end, Set<LocalDate> existingDates) {
        Set<LocalDate> missing = new HashSet<>();
        start.datesUntil(end.plusDays(1)).forEach(date -> {
            DayOfWeek day = date.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY && !existingDates.contains(date)) {
                missing.add(date);
            }
        });
        return missing;
    }
}