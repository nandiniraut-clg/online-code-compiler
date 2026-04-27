package com.compiler.history.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "submissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String language;   // "java", "python", "c", "cpp"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String code;       // the source code submitted

    @Column(columnDefinition = "TEXT")
    private String stdin;      // input passed to the program

    @Column(columnDefinition = "TEXT")
    private String output;     // stdout from execution engine

    @Column(columnDefinition = "TEXT")
    private String stderr;     // stderr / error messages

    @Column(nullable = false)
    private Integer exitCode;  // 0 = success, non-zero = error, 124 = timeout

    @Column(nullable = false)
    private LocalDateTime timestamp;
}
