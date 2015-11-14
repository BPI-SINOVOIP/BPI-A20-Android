#include <pthread.h>
#include <stddef.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

// Ensure that we can restore a stack of a finished thread.

int g_data;

void __attribute__((noinline)) foobar(int *p) {
  *p = 42;
}

void *Thread1(void *x) {
  foobar(&g_data);
  return NULL;
}

void *Thread2(void *x) {
  usleep(1000*1000);
  g_data = 43;
  return NULL;
}

int main() {
  pthread_t t[2];
  pthread_create(&t[0], NULL, Thread1, NULL);
  pthread_create(&t[1], NULL, Thread2, NULL);
  pthread_join(t[0], NULL);
  pthread_join(t[1], NULL);
  return 0;
}

// CHECK: WARNING: ThreadSanitizer: data race
// CHECK:   Write of size 4 at {{.*}} by thread 2:
// CHECK:   Previous write of size 4 at {{.*}} by thread 1:
// CHECK:     #0 foobar
// CHECK:     #1 Thread1
// CHECK:   Thread 1 (finished) created at:
// CHECK:     #0 pthread_create
// CHECK:     #1 main

