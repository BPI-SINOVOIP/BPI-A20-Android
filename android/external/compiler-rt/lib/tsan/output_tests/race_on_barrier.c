#include <pthread.h>
#include <stdio.h>
#include <stddef.h>
#include <unistd.h>

pthread_barrier_t B;
int Global;

void *Thread1(void *x) {
  pthread_barrier_init(&B, 0, 2);
  pthread_barrier_wait(&B);
  return NULL;
}

void *Thread2(void *x) {
  usleep(1000000);
  pthread_barrier_wait(&B);
  return NULL;
}

int main() {
  pthread_t t;
  pthread_create(&t, NULL, Thread1, NULL);
  Thread2(0);
  pthread_join(t, NULL);
  pthread_barrier_destroy(&B);
  return 0;
}

// CHECK:      WARNING: ThreadSanitizer: data race

