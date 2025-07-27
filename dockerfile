# ===================================================================
# GIAI ĐOẠN 1: BUILD (Biên dịch và đóng gói ứng dụng)
# Sử dụng base image JDK 21, khớp với phiên bản trong pom.xml của bạn
# ===================================================================
FROM eclipse-temurin:21-jdk-jammy as builder

# Đặt thư mục làm việc bên trong container
WORKDIR /workspace/app

# Tận dụng Docker cache:
# Chỉ copy những file cần thiết để download dependencies trước
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw
COPY pom.xml .

# Chạy lệnh download dependencies của Maven
# Nếu pom.xml không đổi, layer này sẽ được cache lại, build sẽ nhanh hơn
RUN ./mvnw dependency:go-offline

# Copy toàn bộ source code của bạn vào
COPY src src

# Build ứng dụng thành file JAR, bỏ qua việc chạy test để tăng tốc
RUN ./mvnw clean package -DskipTests

# Tạo một thư mục để giải nén file JAR, phục vụ cho việc tối ưu layer
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

# ===================================================================
# GIAI ĐOẠN 2: RUNTIME (Chạy ứng dụng)
# Sử dụng base image JRE 21 để image cuối cùng nhẹ và an toàn hơn
# ===================================================================
FROM eclipse-temurin:21-jre-jammy

# Đặt thư mục làm việc cho ứng dụng
WORKDIR /app

# Tạo volume cho thư mục /tmp, là một best practice cho ứng dụng Spring Boot
VOLUME /tmp

# Lấy các phần đã được tách ra từ giai đoạn build
ARG DEPENDENCY=/workspace/app/target/dependency

# Copy các thư viện (dependencies)
COPY --from=builder ${DEPENDENCY}/BOOT-INF/lib /app/lib

# Copy metadata của ứng dụng
COPY --from=builder ${DEPENDENCY}/META-INF /app/META-INF

# Copy code đã được biên dịch của bạn
COPY --from=builder ${DEPENDENCY}/BOOT-INF/classes /app

# **FIX LỖI QUAN TRỌNG**: Copy các class loader của Spring Boot
# Đây chính là phần thiếu trong Dockerfile trước, gây ra lỗi ClassNotFoundException
COPY --from=builder ${DEPENDENCY}/org /app/org

# Mở cổng 8000. Cổng này khớp với `server.port=8000` trong file application.properties của bạn.
# Render sẽ dùng cổng này để điều hướng traffic.
EXPOSE 8000

# Lệnh để khởi chạy ứng dụng Spring Boot đã được giải nén
# Classpath bao gồm thư mục hiện tại (.), nơi chứa thư mục /org và code của bạn,
# và tất cả các file jar trong thư mục /lib.
ENTRYPOINT ["java", \
            "-cp", ".:lib/*", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "org.springframework.boot.loader.launch.JarLauncher"]
