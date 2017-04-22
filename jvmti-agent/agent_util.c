#include "agent_util.h"

void
fatal_error(const char * format, ...)
{
  va_list ap;

  va_start(ap, format);
  (void)vfprintf(stderr, format, ap);
  (void)fflush(stderr);
  va_end(ap);
  exit(3);
}

void
check_jvmti_error(jvmtiEnv *jvmti, jvmtiError errnum, const char *str)
{
  if (errnum != JVMTI_ERROR_NONE) {
    char *errnum_str;

    errnum_str = NULL;
    (void)(*jvmti)->GetErrorName(jvmti, errnum, &errnum_str);

    fatal_error("ERROR: JVMTI: %d(%s): %s\n", errnum,
            (errnum_str == NULL ? "Unknown" : errnum_str),
            (str == NULL ? "" : str));
  }
}

void
deallocate(jvmtiEnv *jvmti, void *ptr)
{
  jvmtiError error;

  error = (*jvmti)->Deallocate(jvmti, ptr);
  check_jvmti_error(jvmti, error, "Cannot deallocate memory");
}
