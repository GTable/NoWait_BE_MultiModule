# NoWait
<img width="348" height="137" alt="Logo-primary" src="https://github.com/user-attachments/assets/a6a86342-f526-4e34-9ef1-4f59305551d5" />

## 💡프로젝트 소개
주점 대기 예약부터 메뉴 주문까지 원격으로 처리할 수 있는 웨이팅 서비스입니다.
![NoWait-Architecture](https://github.com/user-attachments/assets/408f468c-4ac2-4eb6-93d5-e39c4d5cd557)


## 📜 프로젝트 목표

### 핵심 목표
- Redis SortedSet기반 대기열으로 급격한 트래픽에도 서비스 중단 없이 대기열 유지
- Redis SPOF 해결
- 멀티 모듈 프로젝트 구조로 서비스 영역을 분리하여 독립 빌드·배포 수행


## 📌 Architecture
### 1️⃣ System Architecture
![NoWait-Architecture](https://github.com/user-attachments/assets/b85dc5fd-0a42-4f1a-a79e-448044a08c47)

### 2️⃣ Multi Module Architecture
![NoWait-MultiModule-Architecture](https://github.com/user-attachments/assets/e30dcaf6-c1dc-4a5b-b2a3-29e22f1536f1)

### 3️⃣ ERD
<img width="3142" height="2188" alt="erd" src="https://github.com/user-attachments/assets/12dfe5f0-ae78-4cc9-80eb-8ccdb1f444ba" />

## 🔌 API 문서
### 관리자 
<a href="http://43.202.201.254:8085/swagger-ui/index.html#/">Admin Server Swagger</a>

### 사용자
<a href="http://43.202.201.254:8081/swagger-ui/index.html#/">User Server Swagger</a>


## 🛠️ 사용 기술

<div> 
  <img src="https://img.shields.io/badge/java-007396?style=for-the-badge&logo=java&logoColor=white">
  <img src="https://img.shields.io/badge/gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white">
  <img src="https://img.shields.io/badge/springboot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white">
  <img src="https://img.shields.io/badge/mysql-4479A1?style=for-the-badge&logo=mysql&logoColor=white">
  <img src="https://img.shields.io/badge/redis-FF4438?style=for-the-badge&logo=redis&logoColor=white">
  <br/>

  <img src="https://img.shields.io/badge/aws-232F3E?style=for-the-badge&logo=amazonwebservices&logoColor=white">
  <img src="https://img.shields.io/badge/ec2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white">
  <img src="https://img.shields.io/badge/rds-527FFF?style=for-the-badge&logo=amazonrds&logoColor=white">
  <br/>
</div>

## 👨‍👩‍👧‍👦 팀원 소개
<table>
    <tr align="center">
        <td><b>Backend</b></td>
        <td><b>Backend</b></td>
    </tr>
    <tr align="center">
        <td>
            <a href="https://github.com/hseong3243">김지훈</a>
        </td>
        <td>
            <a href="https://github.com/seminchoi">정혜민</a>
        </td>
    </tr>
    <tr align="center">
        <td>
            <img src="https://avatars.githubusercontent.com/u/100821696?v=4"
                 width="200" height="200">
        </td>
        <td>
            <img src="https://avatars.githubusercontent.com/u/114489245?v=4"
                 width="200" height="200">
        </td>
    </tr>
</table>
