//package com.printkon.pdp.bootstrap;
//
//import com.printkon.pdp.cms.models.*;
//import com.printkon.pdp.cms.repositories.*;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class DataSeeder {
//
//	@Bean
//	CommandLineRunner seedCmsData(FeatureRepository featureRepository,
//			PrintingServiceRepository printingServiceRepository, TestimonialRepository testimonialRepository,
//			LandingStatRepository landingStatRepository) {
//		return args -> {
//
//			// Avoid duplicate seeding
//			if (featureRepository.count() > 0 || printingServiceRepository.count() > 0
//					|| testimonialRepository.count() > 0 || landingStatRepository.count() > 0) {
//				System.out.println("⚠ CMS data already exists. Skipping seeding.");
//				return;
//			}
//
//			// ---------- Seed Features ----------
//			featureRepository.save(Feature.builder().title("Fast Order Processing")
//					.description("We ensure quick turnaround times on every print job.")
//					.iconUrl("https://localhost:8443/api/images/cms/icons/speed.svg").orderIndex(1).enabled(true)
//					.build());
//
//			featureRepository.save(Feature.builder().title("Premium Print Quality")
//					.description("High-resolution prints using advanced offset and digital technology.")
//					.iconUrl("https://localhost:8443/api/images/cms/icons/quality.svg").orderIndex(2).enabled(true)
//					.build());
//
//			featureRepository.save(Feature.builder().title("Custom Design Support")
//					.description("Professional design assistance for brochures, business cards, and more.")
//					.iconUrl("https://localhost:8443/api/images/cms/icons/design.svg").orderIndex(3).enabled(true)
//					.build());
//
//			// ---------- Seed Printing Services ----------
//			printingServiceRepository.save(PrintingServiceEntity.builder().title("Business Cards")
//					.description("Create professional business cards with vibrant colors and durable finishes.")
//					.iconUrl("https://localhost:8443/api/images/cms/icons/business-cards.svg").orderIndex(1)
//					.enabled(true).build());
//
//			printingServiceRepository.save(PrintingServiceEntity.builder().title("Brochures & Flyers")
//					.description("Promote your business effectively with eye-catching marketing materials.")
//					.iconUrl("https://localhost:8443/api/images/cms/icons/brochure.svg").orderIndex(2).enabled(true)
//					.build());
//
//			printingServiceRepository.save(PrintingServiceEntity.builder().title("Posters & Banners")
//					.description("Make your message stand out with large-format, high-quality prints.")
//					.iconUrl("https://localhost:8443/api/images/cms/icons/banner.svg").orderIndex(3).enabled(true)
//					.build());
//
//			printingServiceRepository.save(PrintingServiceEntity.builder().title("Custom Packaging")
//					.description("Design and print unique packaging solutions for your brand.")
//					.iconUrl("https://localhost:8443/api/images/cms/icons/packaging.svg").orderIndex(4).enabled(true)
//					.build());
//
//			// ---------- Seed Testimonials ----------
//			testimonialRepository
//					.save(Testimonial.builder().author("Rohit Sharma").role("Marketing Head, PrintMart Pvt. Ltd.")
//							.quote("PrintKon delivers exceptional print quality and unmatched reliability every time!")
//							.imageUrl("https://localhost:8443/api/images/cms/testimonials/rohit.jpg").orderIndex(1)
//							.enabled(true).build());
//
//			testimonialRepository.save(Testimonial.builder().author("Anita Desai").role("Founder, Craftify Creations")
//					.quote("Their team helped me design the perfect packaging for my handmade products.")
//					.imageUrl("https://localhost:8443/api/images/cms/testimonials/anita.jpg").orderIndex(2)
//					.enabled(true).build());
//
//			testimonialRepository.save(Testimonial.builder().author("Karan Mehta").role("Entrepreneur")
//					.quote("Superb customer service and attention to detail. Highly recommended!")
//					.imageUrl("https://localhost:8443/api/images/cms/testimonials/karan.jpg").orderIndex(3)
//					.enabled(true).build());
//
//			// ---------- Seed Landing Stats ----------
//			landingStatRepository.save(LandingStat.builder().label("Projects Completed").value("1200+").isDynamic(false)
//					.orderIndex(1).enabled(true).build());
//
//			landingStatRepository.save(LandingStat.builder().label("Happy Clients").value("850+").isDynamic(false)
//					.orderIndex(2).enabled(true).build());
//
//			landingStatRepository.save(LandingStat.builder().label("Years of Experience").value("15+").isDynamic(false)
//					.orderIndex(3).enabled(true).build());
//
//			landingStatRepository.save(LandingStat.builder().label("Print Orders per Month").value("300+")
//					.isDynamic(false).orderIndex(4).enabled(true).build());
//
//			System.out.println("✅ CMS features, printing services, testimonials, and stats seeded successfully!");
//		};
//	}
//}
