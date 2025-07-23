package com.reactiverates.application;

import com.reactiverates.domain.model.HistoricalExchangeRate;
import com.reactiverates.domain.service.HistoricalRateProvider;
import com.reactiverates.domain.service.HistoricalRateRepository;
import com.reactiverates.domain.service.HistoricalRateService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Comparator;
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
        
        return Flux.defer(() -> {
            Flux<HistoricalExchangeRate> dbRates = repository.findByPeriod(fromCurrency, toCurrency, startDate, endDate)
                .doOnNext(rate -> log.debug("Found in DB: {} -> {} = {} on {}", 
                    fromCurrency, toCurrency, rate.rate(), rate.date()));

            Mono<Set<LocalDate>> missingDates = findMissingDates(fromCurrency, toCurrency, startDate, endDate);
            
            Flux<HistoricalExchangeRate> newRates = missingDates
                .filter(dates -> !dates.isEmpty())
                .doOnNext(dates -> log.info("Need to fetch {} missing dates for {} -> {}", 
                    dates.size(), fromCurrency, toCurrency))
                .flatMapMany(dates -> provider.getHistoricalRatesForDates(fromCurrency, toCurrency, dates)
                    .doOnNext(rate -> log.debug("Fetched from API: {} -> {} = {} on {}", 
                        fromCurrency, toCurrency, rate.rate(), rate.date()))
                    .collectList()
                    .flatMapMany(rates -> {
                        if (rates.isEmpty()) {
                            return Flux.empty();
                        }
                        return repository.saveAll(Flux.fromIterable(rates))
                            .doOnNext(saved -> log.debug("Saved to DB: {} -> {} = {} on {}", 
                                fromCurrency, toCurrency, saved.rate(), saved.date()))
                            .onErrorContinue((saveError, obj) -> {
                                if (obj instanceof HistoricalExchangeRate rate) {
                                    log.warn("Failed to save rate for {} on {}, but continuing with other data. Error: {}", 
                                        rate.getCacheKey(), rate.date(), saveError.getMessage());
                                }
                            });
                    })
                    .onErrorResume(error -> {
                        log.warn("Failed to fetch data from provider for {} -> {}, returning only DB data. Error: {}", 
                            fromCurrency, toCurrency, error.getMessage());
                        return Flux.empty();
                    })
                );
            
            return Flux.merge(dbRates, newRates)
                .distinct(rate -> rate.getCacheKey())
                .sort(Comparator.comparing(HistoricalExchangeRate::date))
                .doOnComplete(() -> log.info("Successfully retrieved historical rates for {} -> {} from {} to {}", 
                    fromCurrency, toCurrency, startDate, endDate))
                .doOnError(error -> log.error("Failed to get historical rates for {} -> {}: {}", 
                    fromCurrency, toCurrency, error.getMessage()));
        });
    }
    
    @Override
    public Mono<Boolean> isDataCompleteForPeriod(
            String fromCurrency, 
            String toCurrency, 
            LocalDate startDate, 
            LocalDate endDate) {
        
        log.debug("Checking data completeness for {} -> {} from {} to {}", 
            fromCurrency, toCurrency, startDate, endDate);
        
        return findMissingDates(fromCurrency, toCurrency, startDate, endDate)
            .map(Set::isEmpty)
            .doOnNext(complete -> log.debug("Data complete for {} -> {} from {} to {}: {}", 
                fromCurrency, toCurrency, startDate, endDate, complete));
    }
    
    @Override
    public Mono<Long> getHistoricalDataCount(String fromCurrency, String toCurrency) {
        log.debug("Counting historical data for {} -> {}", fromCurrency, toCurrency);
        
        return repository.countByPair(fromCurrency, toCurrency)
            .doOnNext(count -> log.debug("Found {} historical records for {} -> {}", 
                count, fromCurrency, toCurrency));
    }
    
    @Override
    public Mono<LocalDate[]> getDataDateRange(String fromCurrency, String toCurrency) {
        log.debug("Getting date range for {} -> {}", fromCurrency, toCurrency);
        
        Mono<LocalDate> earliest = repository.findEarliestDate(fromCurrency, toCurrency);
        Mono<LocalDate> latest = repository.findLatestDate(fromCurrency, toCurrency);
        
        return Mono.zip(earliest, latest)
            .map(tuple -> new LocalDate[]{tuple.getT1(), tuple.getT2()})
            .doOnNext(range -> log.debug("Date range for {} -> {}: {} to {}", 
                fromCurrency, toCurrency, range[0], range[1]))
            .switchIfEmpty(Mono.just(new LocalDate[0]));
    }
    
    private Mono<Set<LocalDate>> findMissingDates(
            String fromCurrency, 
            String toCurrency, 
            LocalDate startDate, 
            LocalDate endDate) {
        
        Set<LocalDate> allBusinessDates = generateBusinessDates(startDate, endDate);
        
        return repository.findExistingDates(fromCurrency, toCurrency, startDate, endDate)
            .map(existingDates -> {
                Set<LocalDate> missing = allBusinessDates.stream()
                    .filter(date -> !existingDates.contains(date))
                    .collect(Collectors.toSet());
                
                log.debug("Period {} to {}: {} business days, {} in DB, {} missing", 
                    startDate, endDate, allBusinessDates.size(), existingDates.size(), missing.size());
                
                return missing;
            });
    }
    
    private Set<LocalDate> generateBusinessDates(LocalDate startDate, LocalDate endDate) {
        return startDate.datesUntil(endDate.plusDays(1))
            .filter(date -> {
                int dayOfWeek = date.getDayOfWeek().getValue();
                return dayOfWeek <= 5;
            })
            .collect(Collectors.toSet());
    }
} 